#include <assert.h>
#include <errno.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <syslog.h>

#include <arpa/inet.h>

#include <linux/netfilter_ipv4.h>

#include <netinet/in.h>

#include <sys/file.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/wait.h>

#define BUF_SIZE 4096

char* remoteaddr;
int remoteport;


void cleanup(int sig) {
    syslog(LOG_INFO, "Cleaning up...");
    exit(0);
}

void sigreap(int sig) {
    int status;
    pid_t p;
    while ((p = waitpid(-1, &status, WNOHANG)) > 0) {
        syslog(LOG_INFO, "sigreap: pid=%d, status=%d\n", (int)p, status);
    }
    signal(SIGCHLD, sigreap);
}

int set_nonblock(int fd) {
    int fl;
    int x = fcntl(fd, F_GETFL, &fl);
    if (x < 0) {
        syslog(LOG_ERR, "fcntl F_GETFL: FD %d: %s", fd, strerror(errno));
        exit(1);
    }

    fl |= O_NONBLOCK;
    x = fcntl(fd, F_SETFL, &fl);
    if (x < 0) {
        syslog(LOG_ERR, "fcntl F_SETFL: FD %d: %s", fd, strerror(errno));
        exit(1);
    }
}

int create_server_sock(int port) {
    int addrlen, s, on = 1, x;
    static struct sockaddr_in client_addr;

    s = socket(AF_INET, SOCK_STREAM, 0);
    if (s < 0) {
        perror("socket");
        exit(1);
    }

    addrlen = sizeof(client_addr);
    memset(&client_addr, '\0', addrlen);
    client_addr.sin_family = AF_INET;
    client_addr.sin_addr.s_addr = inet_addr(INADDR_ANY);
    client_addr.sin_port = htons(port);
    setsockopt(s, SOL_SOCKET, SO_REUSEADDR, &on, 4);
    x = bind(s, (struct sockaddr*)&client_addr, addrlen);
    if (x < 0) {
        perror("bind");
        exit(1);
    }

    x = listen(s, 5);
    if (x < 0) {
        perror("listen");
        exit(1);
    }

    return s;
}

int wait_for_connection(int master_sock) {
    static int newsock, len;
    static struct sockaddr_in peer;

    len = sizeof(struct sockaddr);
    newsock = accept(master_sock, (struct sockaddr*)&peer, &len);
    if (newsock < 0) {
        if (errno != EINTR) {
            perror("accept");
        }
    }

    // Where are we going?
    remoteaddr = inet_ntoa(peer.sin_addr);
    remoteport = ntohs(peer.sin_port);

    set_nonblock(newsock);
    return newsock;
}

int create_proxy(int client_sock) {
    struct sockaddr_in server_socket;
    int size = sizeof(server_socket);
    int on = 1;

    if (getsockopt(client_sock, SOL_IP, SO_ORIGINAL_DST, &server_socket, (socklen_t*)&size) < 0) {
        perror("Could not determine socket's original destination");
        return 0;
    }

    int proxy_socket  = socket(AF_INET, SOCK_STREAM, 0);
    if (proxy_socket < 0) {
        return proxy_socket;
    }

    setsockopt(proxy_socket, SOL_SOCKET, SO_REUSEADDR, &on, 4);

    int ret = connect(proxy_socket, (struct sockaddr*)&server_socket, size);
    if (ret < 0) {
        close(proxy_socket);
        return ret;
    }

    set_nonblock(proxy_socket);
    return proxy_socket;
}

int socket_write(int socket, char* buf, int* len) {
    int written = write(socket, buf, *len);
    if (written <= 0) {
        return written;
    }
    if (written != *len) {
        memmove(buf, buf+written, (*len)-written);
    }
    *len -= written;
    return written;
}

int service_client(int client_sock, int server_sock) {
    int maxfd = (client_sock > server_sock ? client_sock : server_sock) + 1;
    char* sbuf = new char[BUF_SIZE];
    char* cbuf = new char[BUF_SIZE];
    int cbo = 0, sbo = 0;
    fd_set R;

    while (1) {
        if (cbo) {
            if (socket_write(server_sock, cbuf, &cbo) < 0 && errno != EWOULDBLOCK) {
                syslog(LOG_ERR, "write %d: %s", server_sock, strerror(errno));
                exit(1);
            }
        }
        if (sbo) {
            if (socket_write(client_sock, sbuf, &sbo) < 0 && errno != EWOULDBLOCK) {
                syslog(LOG_ERR, "write: %d: %s", client_sock, strerror(errno));
                exit(1);
            }
        }

        FD_ZERO(&R);
        if (cbo < BUF_SIZE) {
            FD_SET(client_sock, &R);
        }
        if (sbo < BUF_SIZE) {
            FD_SET(server_sock, &R);
        }

        struct timeval timeout;
        timeout.tv_sec = 0;
        timeout.tv_usec = 1000;

        int ret = select(maxfd+1, &R, 0, 0, &timeout);
        if (ret > 0) {
            if (FD_ISSET(client_sock, &R)) {
                int bytes = read(client_sock, cbuf+cbo, BUF_SIZE-cbo);
                syslog(LOG_INFO, "read %d bytes from CLIENT (%d)", bytes, client_sock);
                if (bytes > 0) {
                    cbo += bytes;
                } else {
                    close(client_sock);
                    close(server_sock);
                    _exit(0);
                }
            }
            if (FD_ISSET(server_sock, &R)) {
                int bytes = read(server_sock, sbuf+sbo, BUF_SIZE-sbo);
                syslog(LOG_INFO, "read %d bytes from SERVER (%d)", bytes, server_sock);
                if (bytes > 0) {
                    sbo += bytes;
                } else {
                    close(client_sock);
                    close(server_sock);
                    _exit(0);
                }
            }
        } else if (ret < 0 && errno != EINTR) {
            close(server_sock);
            close(client_sock);
            _exit(0);
        }
    }
}


int main(int argc, char* argv[]) {
    int client, server;
    int master_sock;

    if (1 != argc) {
        fprintf(stderr, "usage: %s local_port\n", argv[0]);
        exit(1);
    }
    int localport = atoi(argv[1]);

    assert(localport > 0);

    signal(SIGINT, cleanup);
    signal(SIGCHLD, sigreap);

    master_sock = create_server_sock(localport);
    for (;;) {
        if ((client = wait_for_connection(master_sock)) < 0) {
            continue;
        }
        if ((server = create_proxy(client)) < 0) {
            continue;
        }
        if (!fork()) {
            syslog(LOG_INFO, "connecting to %s:%d fd=%d\n", remoteaddr, remoteport, server);
            service_client(client, server);
        }
        close(client);
        close(server);
    }
}

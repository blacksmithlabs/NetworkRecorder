#include <assert.h>
#include <errno.h>
#include <math.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <syslog.h>
#include <unistd.h>

#include <arpa/inet.h>

#include <linux/netfilter_ipv4.h>

#include <netinet/in.h>

#include <sys/file.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/wait.h>

#define BUF_SIZE 4096

FILE* logfile;
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
    int fl = fcntl(fd, F_GETFL, 0);
    if (fcntl(fd, F_SETFL, fl | O_NONBLOCK) < 0) {
        syslog(LOG_ERR, "fcntl F_SETFL: FD %d: %s", fd, strerror(errno));
        exit(1);
    }
}

int create_server_sock(int port) {
    int addrlen, s, on = 1, x;
    struct sockaddr_in client_addr;

    s = socket(AF_INET, SOCK_STREAM, 0);
    if (s < 0) {
        perror("socket");
        exit(1);
    }

    addrlen = sizeof(client_addr);
    memset(&client_addr, '\0', addrlen);
    client_addr.sin_family = AF_INET;
    client_addr.sin_addr.s_addr = INADDR_ANY;
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
    static socklen_t len = sizeof(struct sockaddr);
    struct sockaddr_in peer;

    int newsock = accept(master_sock, (struct sockaddr*)&peer, &len);
    if (newsock < 0) {
        if (errno != EINTR) {
            perror("accept");
        }
    }

    set_nonblock(newsock);
    return newsock;
}

int create_proxy(int client_sock) {
    struct sockaddr_in client_info;
    int size = sizeof(client_info);
    int on = 1;

    if (getsockopt(client_sock, SOL_IP, SO_ORIGINAL_DST, &client_info, (socklen_t*)&size) < 0) {
        perror("Could not determine socket's original destination");
        return 0;
    }

    // Where are we going?
    remoteaddr = inet_ntoa(client_info.sin_addr);
    remoteport = ntohs(client_info.sin_port);

    int proxy_socket  = socket(AF_INET, SOCK_STREAM, 0);
    if (proxy_socket < 0) {
        return proxy_socket;
    }

    setsockopt(proxy_socket, SOL_SOCKET, SO_REUSEADDR, &on, 4);

    int ret = connect(proxy_socket, (struct sockaddr*)&client_info, size);
    if (ret < 0) {
        close(proxy_socket);
        return ret;
    }

    set_nonblock(proxy_socket);
    return proxy_socket;
}

void write_hex(char* buf, int len) {
    // 8 bytes per line, 41 chars per line
    // ddd  xx xx xx xx  xx xx xx xx  dddd dddd\n
    int lines = (int)ceil(len/8.0);
    int hexlen = lines * 41;

    char* hex = new char[hexlen+2];

    int bufpos = 0;
    char* hexpos = hex;
    for (int i=0;i<lines;i++) {
        sprintf(hexpos, "%03X  ", i*8);
        hexpos += 5;

        int ploc = 25;
        for (int b=0; b<8; b++) {
            sprintf(hexpos, "%02X ", (unsigned char)buf[bufpos]);
            hexpos += 3;
            ploc -= 2;

            if (buf[bufpos] > 0x20 && buf[bufpos] < 0x7F) {
                *(hexpos+ploc) = buf[bufpos];
            } else {
                *(hexpos+ploc) = '.';
            }

            if (b == 3) {
                *(hexpos++) = ' ';
                *(hexpos+ploc) = ' ';

            } else if (b == 7) {
                *(hexpos++) = ' ';
            }

            bufpos++;
            if (bufpos >= len) {
                for (int c=b+1; c<8; c++) {
                    *(hexpos++) = ' ';
                    *(hexpos++) = ' ';
                    *(hexpos++) = ' ';
                    ploc -= 3;
                    if (c == 3) {
                        *(hexpos++) = ' ';
                        ploc--;
                    } else if (c == 7) {
                        *(hexpos++) = ' ';
                    }
                }
                break;
            }
        }

        hexpos += ploc;
        *(hexpos++) = '\n';
    }
    *(hexpos++) = '\n';
    *hexpos = '\0';

    fwrite(hex, sizeof(char), hexpos-hex, logfile);
    delete hex;
}

int socket_write(int socket, char* buf, int* len, bool isServer) {
    int written = write(socket, buf, *len);
    if (written <= 0) {
        return written;
    }

    fprintf(logfile, "%s (%s:%d) [%d]\n", (isServer ? "S>C" : "C>S"), remoteaddr, remoteport, written);
    write_hex(buf, written);

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
            if (socket_write(server_sock, cbuf, &cbo, false) < 0 && errno != EWOULDBLOCK) {
                syslog(LOG_ERR, "write %d: %s", server_sock, strerror(errno));
                exit(1);
            }
        }
        if (sbo) {
            if (socket_write(client_sock, sbuf, &sbo, true) < 0 && errno != EWOULDBLOCK) {
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

    if (argc != 3) {
        fprintf(stderr, "usage: %s local_port log_file\n", argv[0]);
        exit(1);
    }
    int localport = atoi(argv[1]);
    logfile = (strcmp(argv[2], "stdout") == 0) ? stdout : fopen(argv[2], "a");
    if (logfile == NULL) {
        fprintf(stderr, "log_file %s could not be opened\n", argv[2]);
        exit(2);
    }

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

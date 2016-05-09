#include <stdio.h>

extern void Xmain(void);
extern void XinitGlobals();


int main(int argc, char** argv) {
    XinitGlobals();
    Xmain();
    return 0;
}

void Xprint(int val) {
    printf("output: %d\n", val);
}

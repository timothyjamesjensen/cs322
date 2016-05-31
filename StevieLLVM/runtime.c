#include <stdio.h>
#include <stdlib.h>

extern void Xmain(void);
extern void XinitGlobals(void);

int main(int argc, char** argv) {
    XinitGlobals();
    Xmain();
    exit(0);
    return 0;
}

void Xprint(int val) {
    printf("output: %d\n", val);
}

void* XallocArray(int num, int size) {
    return (void*)malloc(num*size);
}

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
//    num = num + 1; // increase the array size in order to store the length

    int* array = malloc(++num*size);
    
    // if malloc returns null
    if (array == NULL) {
      printf("Out of Memory");
      exit(1);
    }
    //store the length of the array in the first slot
    *array = --num;
    return (void*)array;
}

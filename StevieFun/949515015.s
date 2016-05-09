        .file   "949515015.s"

        .data
Xe:
        .long   0
Xque:
        .long   0
Xarc:
        .long   0
Xw:
        .long   0

        .text
        .globl  XinitGlobals
XinitGlobals:
        pushq   %rbp
        movq    %rsp, %rbp
        movl    $54, %eax
        movl    $78, %edi
        imull   %edi, %eax
        movl    %eax, Xe(%rip)
        movl    $1, %eax
        movl    %eax, Xque(%rip)
        movl    $0, %eax
        movl    %eax, Xarc(%rip)
        movl    $11, %eax
        movl    %eax, Xw(%rip)
        movq    %rbp, %rsp
        popq    %rbp
        ret

        .globl  Xpudding
Xpudding:
        pushq   %rbp
        movq    %rsp, %rbp
        movl    %esi, %eax
        movl    %esi, %r10d
        subl    %r10d, %eax
        movl    16(%rbp), %r10d
        xchgl   %r10d, %eax
        subl    %r10d, %eax
        pushq   %rax
        movl    Xque(%rip), %eax
        orl     %eax, %eax
        jz      l0
        pushq   %r9
        pushq   %r8
        pushq   %rdx
        pushq   %rcx
        pushq   %rsi
        pushq   %rdi
        movl    $949515015, %edi
        call    Xprint
        popq    %rdi
        popq    %rsi
        popq    %rcx
        popq    %rdx
        popq    %r8
        popq    %r9
l0:
        movl    %edi, %eax
        movl    %edx, %r10d
        subl    %r10d, %eax
        movl    %r8d, %r10d
        xchgl   %r10d, %eax
        subl    %r10d, %eax
        movl    $4, %r10d
        imull   %r10d, %eax
        pushq   %rax
        movl    -16(%rbp), %eax
        movl    -8(%rbp), %r10d
        cmpl    %eax, %r10d
        jl      l1
        movl    $0, %eax
        jmp     l2
l1:
        movl    $1, %eax
l2:
        movq    %rbp, %rsp
        popq    %rbp
        ret

        .globl  Xmain
Xmain:
        pushq   %rbp
        movq    %rsp, %rbp
        jmp     l4
l3:
        movl    Xe(%rip), %edi
        call    Xprint
        movl    Xe(%rip), %eax
        movl    $8, %edi
        addl    %edi, %eax
        movl    %eax, Xe(%rip)
l4:
        movl    Xw(%rip), %eax
        movl    Xe(%rip), %edi
        cmpl    %eax, %edi
        jl      l3
        movl    Xw(%rip), %edi
        movl    $7, %esi
        imull   %esi, %edi
        movl    $2016, %esi
        xchgl   %esi, %edi
        subl    %esi, %edi
        call    Xprint
        movl    Xw(%rip), %edi
        movl    $13, %esi
        subl    %esi, %edi
        movl    $102, %esi
        call    Xante
        movq    %rax, %rdi
        call    Xprint
        movq    %rbp, %rsp
        popq    %rbp
        ret

        .globl  Xante
Xante:
        pushq   %rbp
        movq    %rsp, %rbp
        movl    %esi, %eax
        movl    Xe(%rip), %ecx
        subl    %ecx, %eax
        pushq   %rax
        pushq   %rsi
        pushq   %rdi
        movl    -8(%rbp), %edi
        movl    $1, %esi
        addl    %esi, %edi
        call    Xunicorn
        popq    %rdi
        popq    %rsi
        movq    %rbp, %rsp
        popq    %rbp
        ret

        .globl  Xunicorn
Xunicorn:
        pushq   %rbp
        movq    %rsp, %rbp
        movl    %edi, %eax
        movl    $2, %esi
        cmpl    %eax, %esi
        jnl     l5
        movl    %edi, %eax
        movl    Xw(%rip), %esi
        imull   %esi, %eax
        movq    %rbp, %rsp
        popq    %rbp
        ret
l5:
        pushq   %rdi
        movl    $84, %edi
        movl    -8(%rbp), %esi
        imull   %esi, %edi
        call    Xunicorn
        popq    %rdi
        movq    %rbp, %rsp
        popq    %rbp
        ret


	.file	"demo.s"

	.data

	.text
	.globl	XinitGlobals
XinitGlobals:
	pushq	%rbp
	movq	%rsp, %rbp
	movq	%rbp, %rsp
	popq	%rbp
	ret

	.globl	Xmain
Xmain:
	pushq	%rbp
	movq	%rsp, %rbp
	movl	$25, %eax
	pushq	%rax
	leaq	-8(%rbp), %rdi   # load the address
	movl	$23, %eax        
        pushq   %rax             # push 23 onto stack
        leaq    -16(%rbp), %rsi  # load the address
	call	Xbyref
	movq	%rax, %rdi
	call	Xprint
	movl	-8(%rbp), %edi
	call	Xprint
	movq	%rbp, %rsp
	popq	%rbp
	ret

	.globl	Xbyref
Xbyref:
	pushq	%rbp
	movq	%rsp, %rbp
	pushq	%rsi
	pushq	%rdi
	movq	-16(%rbp), %rdi # load the address
	call	Xredirect
	popq	%rdi
	popq	%rsi
	pushq	%rsi
	pushq	%rdi
	movq	-8(%rbp), %rdi  # load the address
	call	Xredirect
	popq	%rdi
	popq	%rsi
	movl	(%rdi), %eax    # rdi instead of edi
	movl	(%rsi), %ecx    # rsi instead of esi
	addl	%ecx, %eax
	movq	%rbp, %rsp
	popq	%rbp
	ret

	.globl	Xredirect
Xredirect:
	pushq	%rbp
	movq	%rsp, %rbp
	pushq	%rdi
	movq	-8(%rbp), %rdi  # rdi instead of edi
	call	Xincrement
	popq	%rdi
	movq	%rbp, %rsp
	popq	%rbp
	ret

	.globl	Xincrement
Xincrement:
	pushq	%rbp
	movq	%rsp, %rbp
	movl	(%rdi), %eax    # get value from address stored in rdi
	movl	$1, %esi
	addl	%esi, %eax
	movl	%eax, (%rdi)    # store value in address at rdi
	movq	%rbp, %rsp
	popq	%rbp
	ret


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
	movl	-8(%rbp), %edi
	movl	$23, %esi
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
	movl	-16(%rbp), %edi
	call	Xredirect
	popq	%rdi
	popq	%rsi
	pushq	%rsi
	pushq	%rdi
	movl	-8(%rbp), %edi
	call	Xredirect
	popq	%rdi
	popq	%rsi
	movl	%edi, %eax
	movl	%esi, %ecx
	addl	%ecx, %eax
	movq	%rbp, %rsp
	popq	%rbp
	ret

	.globl	Xredirect
Xredirect:
	pushq	%rbp
	movq	%rsp, %rbp
	pushq	%rdi
	movl	-8(%rbp), %edi
	call	Xincrement
	popq	%rdi
	movq	%rbp, %rsp
	popq	%rbp
	ret

	.globl	Xincrement
Xincrement:
	pushq	%rbp
	movq	%rsp, %rbp
	movl	%edi, %eax
	movl	$1, %esi
	addl	%esi, %eax
	movl	%eax, %edi
	movq	%rbp, %rsp
	popq	%rbp
	ret


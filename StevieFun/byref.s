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

# OUTPUT JUSTIFICATION
#
# This assembly file produces the following output:
#
# output: 50
# output: 26
#
# This output is without a doubt the expected output.
# The values 25 and 23 are passed by reference into
# the byref() method. These values are then each passed
# by reference into redirect and increment, which adds
# 1 to the value of each. Since we are using pass by
# reference, the ACTUAL value of these integers is expected
# to change, and this new value will persist outside of the
# method where they are incremented.
#
# 25 + 23 = 48. Since 25 and 23 were both incremented by 1,
# the new values should be 26 and 24. 
# 26 + 24 = 50 which is what is the output generated for x + y.
# x is now 26 which is the output for x.

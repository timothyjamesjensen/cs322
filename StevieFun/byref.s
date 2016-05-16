# Timothy Jensen

# Question 3 Solutions Guide.
#
# 1) Highlight the changes I made to support
#    call by referense. All of the changes I
#    have made have a comment on the RIGHT hand side
#    of the assembly instruction, explaining
#    what it does.
#
# 2) Describe the result produced by running
#    the moddified assembly and justify that it
#    is correct. At the bottom of this file there
#    is a large comment tiled OUTPUT JUSTIFICATION.
#    
# 3) Identify three or more distinct examples of 
#    problems with the original generated code.
#    These are labeled as requested. ex: OPPORTUNITY #
#

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
	leaq	-8(%rbp), %rdi   # load the address of where 25 is stored
	movl	$23, %eax        
        pushq   %rax             # push 23 onto stack
        leaq    -16(%rbp), %rsi  # load the address of where 23 is stored
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
# OPPORTUNITY 1
#
#
# In the following assembly code, the registers
# rsi and rdi are saved to the stack multiple
# times. They are saved to the stack every time
# that there is a function call(twice) because the
# compiler can't tell if the function call will 
# modify the values in the registers and it is
# trying to preserve the values. However this is 
# wasted effort if the function call doesn't use a
# saved register. How can we avoid this wasted effort
# and cut down on the total number of saves?
#
# The answer is by using caller and callee saves.
# We can designate some registers to store temporary
# values that can be used by either the caller or the
# callee. For example, we could change the assembly to 
# look something like this to handle %rsi
# 
# pushq %r12
# ...
# movl %esi, %r12d
# ...
# call xredirect
# ...
# call xredirect
# ...
# popq %r12d
#
# We can use this technique to cut down on the stack space
# being uses as well as REDUCE the total number of saves.

	pushq	%rsi
	pushq	%rdi
	movq	-16(%rbp), %rdi # load the address of where 25 is stored
	call	Xredirect
	popq	%rdi
	popq	%rsi
	pushq	%rsi
	pushq	%rdi
	movq	-8(%rbp), %rdi  # load the address of where 23 is stored
	call	Xredirect
	popq	%rdi
	popq	%rsi
	movl	(%rdi), %eax    # get the value in memory at the address in rdi
	movl	(%rsi), %ecx    # get the value in memory at the address in rsi
	addl	%ecx, %eax
	movq	%rbp, %rsp
	popq	%rbp
	ret

	.globl	Xredirect
Xredirect:
	pushq	%rbp
	movq	%rsp, %rbp
	pushq	%rdi
	movq	-8(%rbp), %rdi  # store the address in rdi
	call	Xincrement
	popq	%rdi
	movq	%rbp, %rsp
	popq	%rbp
	ret

# OPPORTUNITY 2
#
#
# This opportunity for enhancement has to do
# with the entire increment function, so I am
# placing this comment directly above where that 
# function begins.
#
# In assembly, there is a four instruction overhead
# for each function that uses the base pointer. This
# overhead increases execution time and prevents the
# use of rbp as a general purpose register. While this
# may not be much of a problem for large functions,
# increment is a small function that does not even make
# use of the stack frame. The four instruction overhead
# to build a stack frame that we don't even use is a 
# complete waste. 
#
# The solution to this would be to generate assembly that
# performs inline calls to implement and avoids the
# useless four instruction overhead. If the compiler 
# could recognize this and take action, that would be a 
# big improvement.

	.globl	Xincrement
Xincrement:
	pushq	%rbp
	movq	%rsp, %rbp
# OPPORTUNITY 3
#
#
# The following portion of assembly code
# gets the value stored in memory from the
# address in rdi and places that value in the
# register eax. It then places the value 1 in
# the register esi, and then it adds esi to eax.
# The process is completed by updating the value
# in memory at the address in rdi with the value
# stored in eax. It takes FOUR lines of assembly 
# to add 1 to the value stored in memory at the 
# address in rdi.
#
# movl (%rdi), %eax
# movl $1, %esi
# addl %esi, %eax
# movl %eax, (%rdi)
#
# These FOUR lines of assembly could be replaced
# with a much more efficient
# SINGLE line of assembly that would accomplish
# exactly the same thing quicker and using fewer
# registers.
#
# addl $1, (%rdi)

	movl	(%rdi), %eax    # get value in memory at the address in rdi
	movl	$1, %esi
	addl	%esi, %eax
	movl	%eax, (%rdi)    # update the value in memory at the address in rdi
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
# reference, the ACTUAL stored value in memory of these integers is expected
# to change, and this new value will persist outside of the
# method where they are incremented because we are updating
# the value stored in memory at the variable address.
#
# 25 and 23 are the ORIGINAL values. 25 + 23 = 48. 
# Since 25 and 23 were both incremented by 1,
# the UPDATED values should be 26 and 24. 
# 26 + 24 = 50 which is what is the output generated for x + y.
# x is now 26 which is the output for x.

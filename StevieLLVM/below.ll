define void @XinitGlobals() {
entry:
  ret void
}

define void @Xmain() {
entry:
  ; call the below function and store the result in t0
  %t0 = call i32 @below(i32 3, i32 4, i32 5)
  call void @Xprint(i32 %t0)
  ret void
}

; declare external runtime library functions
declare void @Xprint(i32)

; function definitions

; define below as a function that returns an i32 int 
; and takes in 3 i32 ints as parameters
; NOTE: xe is x entry
define i32 @below(i32 %xe, i32 %y, i32 %z) {
entry:
  ; immediately jump to the the while loop conditional 
  br label %tst0

; tst0 is is the left side of the &&
tst0:
  ; use phi to preserve SSA form
  ; only needed for x because only x changes
  %x = phi i32 [ %xe, %entry ], [ %xb, %body ]

  ; if x is less than y, test the right hand side
  ; else jump to done
  %cmp0 = icmp slt i32 %x, %y
  br i1 %cmp0, label %tst1, label %done

; tst1 is the right side of the &&
tst1:
  ; if x is less than z, jump to the body
  ; else jump to done
  %cmp1 = icmp slt i32 %x, %z
  br i1 %cmp1, label %body, label %done

body:
  ; set xbody to equal x + 1
  %xb = add i32 %x, 1
  ; jump to the while loop conditional
  br label %tst0

done:
  ret i32 %x
}

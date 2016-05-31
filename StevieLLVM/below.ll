
define void @XinitGlobals() {
entry:
  ret void
}

define void @Xmain() {
entry:
  %t0 = call i32 @below(i32 3, i32 4, i32 5)
  call void @Xprint(i32 %t0)
  ret void
}

; declare external runtime library functions
declare void @Xprint(i32)

; function definitions
define i32 @below(i32 %xe, i32 %y, i32 %z) {
entry:
  br label %tst0
tst0:
  %x = phi i32 [ %xe, %entry ], [ %xb, %body ]

  %cmp0 = icmp slt i32 %x, %y
  br i1 %cmp0, label %tst1, label %done
tst1:
  %cmp1 = icmp slt i32 %x, %z
  br i1 %cmp1, label %body, label %done
body:
  %xb = add i32 %x, 1
  br label %tst0
done:
  ret i32 %x
}

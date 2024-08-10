# Call By Something

<br>

## 배경
- 서비스에서 약 백오십만의 유저에게 알림을 전달하는 새로운 시스템에 대해 이야기를 하다, 어떤 방식으로 이걸 풀어야 하는지 생각하게 됨
- API로 받아줄 경우 대상을 매번 추리는데 리소스가 크게 들거라 생각
- 동시성 모델로 해당 대상 목록을 주기적으로 관리하며 알림을 전달하는건 어떨까 고민하게됨

<br>

## 잘못된 문제 인식 및 커뮤니케이션 오류
- 동시성 모델로 선형적 자료형을 관리하고, 호출시마다 해당 자료형을 청크로 찍어내어 알림을 전달한다.
  - 예상된 문제는 값복사가 일어나서 메모리 사용량이 늘어날 거라 생각됨. 
  - 대상을 추리기 위해서는 값복사 후 청크를 해야하나 vs 값복사 없이 청크를 말까
- 안해도 된다고 해도 자바는 함수호출시 매개변수 전달로 인해 값복사가 일어나는데, 이건 문제가 아닐까?

<br>

## 체크
- 이게 진실이 맞는지 아닌지 갑자기 생각이 들었고, 자바는 call by value인걸 인지는 하고 있지만, 그게 값복사는 아닌듯 하여 확인하고자 함

<br>

## 과정

<br>

#### 우선 아래 코드를 통해 150만개의 대상을 만들고 이걸 리스트에 저장함
```kotlin
val callBySomething = CallBySomething()
val list = callBySomething.list
println("hash=" + System.identityHashCode(list))
for(i in 1500000..3000000){
    list.add(BigInteger.valueOf(i.toLong()))
}
```

<br>

#### 그리고 메모리 사용량을 체크
```kotlin
fun getMemoryUsage(): Long {
    val runtime = Runtime.getRuntime()
    return runtime.totalMemory() - runtime.freeMemory()
}
```
```
# 결과
hash=258952499
Memory used by myList: 120 MB
```

<br>

#### 그리고 해당 객체를 매개변수로 사용하는 함수를 콜하여 해시값과 다시 한번 메모리 사용량을 체크
```kotlin
callBySomething(list)
val memoryAfter2 = getMemoryUsage()
println("Memory used by myList: ${(memoryAfter2 - memoryBefore)/1024/1024} MB")

fun callBySomething(list: MutableList<BigInteger>): MutableList<BigInteger> {
    println("hash=" + System.identityHashCode(list))
    return list
}
```

```
# 결과
hash=258952499
Memory used by myList: 120 MB
# 동일한 해시값과 변화없는 메모리 사용량
```

#### 이번에는 깊은 복사를 해보자
```kotlin
val newList = copy(list)
val memoryAfter3 = getMemoryUsage()
println("Memory used by myList: ${(memoryAfter3 - memoryBefore)/1024/1024} MB")

fun copy(list: MutableList<BigInteger>): List<BigInteger> {
    return list.map { BigInteger(it.toByteArray())  }.also {
        println("hash=" + System.identityHashCode(it))
    }
}
```
```
# 결과
hash=1149319664
Memory used by myList: 236 MB
# 당연히 변경된 해시값과 그리고 증가한 메모리양
```

#### 만약 copy의 코드가 아래와 같다면 list는 새로운 객체를 가리키겠지만, 그 안의 객체는 동일한 객체를 사용하여, 메모리 사용량에 차이가 거의 없음
```kotlin
// 왜냐하면 BigInteger도 객체이기 때문
// 새로운 list를 생성시 기존 BigInteger의 참조를 기반으로 생성
fun copy(list: MutableList<BigInteger>): List<BigInteger> {
    return list.map { it }.also {
        println("hash=" + System.identityHashCode(it))
    }
}
```


#### 자, 그럼 이제 더 헷갈리는 건, 자바는 분명 call by value인데, 왜 마치 call by reference처럼 보이는걸까?
- 분명한건 원시형 자료를 전달할때는 값복사를 한다는 점
- 하지만 그게 아닐 경우에는 값의 참조를 복사하여 전달한다는 점
- 하지만 그렇다고해서 포인터처럼 값의 참조 자체를 전달하는건 아님
- 이게 어떤 차이가 있느냐, 아래에서 확인해보자
```kotlin
fun referenceOfValue(list: MutableList<BigInteger>) {
      var list2 = list
      println("list2 hash=" + System.identityHashCode(list2))
      list2 = mutableListOf()
      println("list2 hash=" + System.identityHashCode(list2))
      println("list=${list.size}")
  }
```
```
# 결과
list2 hash=258952499
list2 hash=2093631819
list=1500001
```
- 일단 코틀린은 매개변수가 불변이라 직접 수정은 못 하기에, 따로 변수에 할당하여 동일한 참조를 같는 변수를 만들어서 수정을 함
- 만약 참조 자체를 전달하는거라면 해당 참조변수를 재선언 했을대, 참조주소 자체에 값을 할당을 하기에 변화가 일어난다
- 하지만 자바는 재선언시 참조에 새로운 값을 할당하는게 아니라, 객체를 새로 생성하고 해당 객체의 참조를 재선언하는 필드에 할당하다보니 포인터를 사용하는것과는 차이가 있다
- 이걸 한번 간단하게 포인터가 사용가능한 golang으로 확인해보자
```go
func main() {
  str := "Hello, Go!"
  printString(&str)
  fmt.Println(str)
}

func printString(org *string) {
  fmt.Println(*org)
  cop := org
  fmt.Println(*cop)
  *cop = "Hello, Go2!"
  fmt.Println(*cop)
}
```
```
# 결과
Hello, Go!
Hello, Go!
Hello, Go2!
Hello, Go2!
```
- 위에서 확인 할 수 있듯이, 원본을 값을 주소 연산자를 통해 받고 그걸 복사 후 포인터로 참조하여 값을 변경하면 원본도 변경된다


## 결론
- 자바는 call by value이지만, 값의 참조를 복사하여 전달한다
- 그렇기에 선언된 값을 매개변수로 사용한다고해도 값의 참조를 복사하여 전달하기에 메모리 사용량이 증가하지 않는다
- 더불어 동시성모델로 관리하여 청크를 통해 실행한다면 BigInteger 또한 객체이기에 청크시 동일한 BigIneger의 참조값을 활용하기에 메모리 이슈도 없을거라 예상된다

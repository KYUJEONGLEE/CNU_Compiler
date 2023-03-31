func myFunction(x int) int
{
    return 5 + x
}

func main()
{
    var a int = 1
    var b int = 0
    if (a and !b)
    {
        a = myFunction(a + 1)
        _print(a)
    } else {
        _print(10)
    }
}
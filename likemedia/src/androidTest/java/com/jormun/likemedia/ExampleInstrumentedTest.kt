package com.jormun.likemedia

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.jormun.likemedia", appContext.packageName)
    }

    //测试，手写一个哥伦布解码示例
    @Test
    fun testGolombCode() {
        val testNum = 6
        //首先，一个像素的信息实际上就是颜色信息，而颜色信息在计算机里面的表达范围是0~255。
        //0~255的二进制范围也就是 0000 0000 - 1111 1111。
        //另外，从Byte数组里面每次读取也只是一个字节。
        //但是java里面，一个int数据是4字节，假设int = 5就为 0000 0000 0000 0000 0000 0000 0000 0101。
        //我们显然不需要这么多，因为我们每次读取只读取一字节，同时颜色位数也不需要那么多，我们只需要一字节数据也就是8位即可。
        //那么就可以&上 0000 0000 0000 0000 0000 0000 1111 1111，也就是0xff了(0x是十六进制的表达前缀)。
        //这样前面的0可以屏蔽掉我们不需要的位数(0&任何数=0)，后面8个1可以保留我们需要的后8位(1&1才等于1)。
        val binNum = testNum and 0xff
        //假设从第三位开始读
        var startBit = 3
        //统计前面0的位数
        var zeroNum = 0
        //循环读，直到8位末尾
        while (startBit < 8) {
            //用我们上面获取到的入参8位二进制数，&上 1000 0000也就是 0x80了。
            //实际上就是暴力求出到底第几位才等于1。
            //第一遍 & 1000 0000
            //第二遍 & 0100 0000    --> & 0x80>>1
            //第三遍 & 0010 0000    --> & 0x80>>2
            //第四遍 & 0001 0000    --> & 0x80>>3
            //...........
            //以此类推，后面的蒙版数，每次只需要0x80右移一位即可。
            //如果原始数据该位为0，得出全0也就是0000 0000，说明没找到。
            //只要结果不为0(不需要管是多少)，说明蒙版数当前这个1&上的位里是1，则为有效数据找到目标。
            if (binNum and (0x80 shr startBit) != 0) {
                //找到了也要移动指针，因为这个指针是指向需要读取的位，而不是已读取的位。
                startBit++
                break
            }
            //没找到，0的位数+1
            zeroNum++
            //指针往右移动一位
            startBit++
        }
        println("zero num: $zeroNum")
        println("current index num: $startBit")


        //到这里就代表我们已经找到0的位数和我们需要读取的起始位。
        //我们把数据拆解成两部分来看，以我们的例子，0 0101为例。
        //拆分为 “0 01” 和 “101”两部分来看。
        //因为哥伦布编码是往前面添加0，所以最高位添加的0再加上后面的第一个1，就永远是1，也就是我们例子里面的 “0 01”。
        //虽然它目前是1，但是这其实是原始数据的最高位，因为哥伦布编码后添加了N个0导致它往后挪了N位。
        //所以我们要还原这部分数据也很简单，直接位移回去添加的0的个数位就行了，而0的位数我们上面已经求出来。

        val headNum = (1 shl zeroNum)//把1左移回去还原最高位。

        var vCountNum = 0//后续部分有效位个数(1)的和。

        //然后求后面那部分数据，还是老规矩，直接用蒙版数求出来就行，而蒙版数跟上面是一样的，只不过还需要继续往右移。
        //右移的次数就是我们上面得出的0的位数。
        for (i in 0 until zeroNum) {//控制指针右移次数
            //因为我们需要收集后面的二进制位，所以指针右移一位，这个总数就要左移一位。
            //把当前的总数左移1位并且重新赋值，比如 1 -> 10，从二进制来看等于先往低位填上0先，从十进制来看就是乘以2。
            vCountNum =  vCountNum shl 1  //左移等同于x2
            println("vCountNum plus: I=${i}, $vCountNum")

            //之所以还需要%8，是因为这指针是作用于一整大串二进制，模8可以确保每次都在8的整数倍或者8以内。
            //比如现在指针已经读取到27了，我们的蒙版数起始始终是 1000 0000，我们不可能右移27位。
            //所以27%8...3，余数是3，所以就可以求出正确的右移位数，同时也确保了每次右移的位数都是在8以内。
            if (binNum and (0x80 shr (startBit % 8)) != 0) {
                //如果不为0，那就+1，等于 +2^0，也就是2的0次方，等于把刚才填上去的低位0变成1
                vCountNum++
            }
            //指针继续往下一位移动
            startBit++
        }

        //把头尾两数相加即可，哥伦布还要-1。
        val result = (headNum + vCountNum) - 1

        println("result num: $result")

        //下面这两个可以打印二进制String(但是会忽略前面的0)
        /*println("5 binNum = ${Integer.toBinaryString(testNum)}")
        println("5 binNum = ${Integer.toBinaryString(0xff)}")*/


    }
}
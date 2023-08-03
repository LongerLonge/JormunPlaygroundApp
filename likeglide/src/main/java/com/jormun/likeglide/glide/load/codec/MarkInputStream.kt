package com.jormun.likeglide.glide.load.codec

import com.jormun.likeglide.glide.cache.ArrayPool
import java.io.InputStream

/**
 * 这是我们自己实现的一个InputStream。
 * 具有 "标记(mark)-重置(reset)" 功能，同时内部集成了我们的数组缓存池。
 * 这么做的好处就是，可以缓存已读取的部分，加上已经有数组缓存池的存在，我们就不需要频繁的创建数组，需要用的时候直接去缓存池取就完事了。
 * 如果一个图片在Decode阶段，频繁的创建数组也是会造成内存抖动的。
 * 而且也大大提升了同size图片的加载速度。
 */
class MarkInputStream(private val inputStream: InputStream, private val arrayPool: ArrayPool) :
    InputStream() {
    private var BUFFER_SIZE_BYTES = 64 * 1024//默认的buffer长度

    private var markPos = -1//标记位置，可以理解为回溯点，-1为无效，0为最左端，大于0代表在数组的某个位置为回溯点
    private var pos = 0//当前读取的下标，每调一次read()、read(byte[])、read(b,start,end)都会发生移动
    private var readCount = 0//读取的数据总长度，可以理解为buffer目前的有效数据长度

    //buffer数组，用来缓存InputStream中已读取的数据，这样就不需要再次读取这部分，提高效率。
    //Stream只能被读取一次
    private var buf: ByteArray? = arrayPool.get(BUFFER_SIZE_BYTES)//初始化时从数组缓存池里面取

    /**
     * read每次读取一个字节返回，即8位二进制(0或者1)。
     * 每次调用这个方法，读取下标都会发生位移。
     */
    override fun read(): Int {

        //被reset pos小于已读 直接从buf中拿
        if (pos < readCount) {
            return buf!![pos++].toInt()
        }
        //这里执行，说明已经读取到readCount位置(pos = readCount)，需要从源Input中取数据了
        val b = inputStream.read()//读取一个字节
        //没有更多数据
        if (b == -1) {
            return b
        }
        //因为要把已读取的部分保存到buffer中
        //判断是否有足够空间存储，pos如果大于buffer长度代表需要扩容，这里是超出一个字节了
        if (pos >= buf!!.size) {
            resizeBuf(0)//扩容
        }
        //都没有问题，那就把读取出来的一个字节加入到buffer中，同时pos下标移动1位
        buf!![pos++] = b.toByte()
        //已读下标移动1位
        readCount++
        return b


    }

    /**
     * 对buffer进行扩容
     * 默认是直接按传入的基数扩容两倍
     * @param len :需要扩容的基数长度
     */
    private fun resizeBuf(len: Int) {
        val newLen = buf!!.size * 2 + len
        val newBuf = arrayPool.get(newLen)
        //拷贝数据
        //buf!!.copyInto(newBuf, 0, 0, buf!!.size)
        System.arraycopy(buf!!, 0, newBuf, 0, buf!!.size)
        //加入数组池，方便下次取出复用
        arrayPool.put(buf!!)
        buf = newBuf
    }

    /**
     *把数据保存到传入的buffer数组。
     * 比如  a.read(byteArray), 就是把a中的stream数据写入到byteArray中。
     * 这个方法被调用时，读取下标会发生位移。
     * @param b : 需要被写入的buffer缓存数组
     */
    override fun read(b: ByteArray): Int {
        return read(b, 0, b.size)
    }

    /**
     * 把数据写入到buffer数组。
     * 这个方法被调用时，读取下标会发生位移。
     * @param b 需要被写入的缓存数组
     * @param off 起始点
     * @param len 需要写入的长度
     */
    override fun read(b: ByteArray, off: Int, len: Int): Int {

        var tempOff = off
        //需要读取的总长度
        var count = len - tempOff
        //buf中的有效数据
        //可能会有人奇怪，已读位置(readCount)和当前读取位置(pos)难道不是相等的吗？
        //别忘了，pos是有可能被reset()方法给重置的，所以这两者并不是总是相等。
        val availables = readCount - pos
        //满足读取的需求，也就是pos到已读下标的距离大于读取长度
        if (availables >= count) {
            //复制数据
            //开始写入，可以理解为：
            // 从 b数组 的 tempOff 作为起始写入下标(点)，把该buffer的 pos 到 count 位置的数据写进去
            //可能有人会奇怪，为什么要从pos开始，不从0开始吗？
            //因为pos代表上一次已经读到的位置，说明已经将0->pos的数据给读出去了，所以我们这里就应该是从pos开始读
            System.arraycopy(buf!!, pos, b, tempOff, count)
            // TODO: 这里不知道为什么用copyInto图片会错位错色，其它地方倒不会，非常的诡异，暂且搁置
            //buf!!.copyInto(b, tempOff, pos, count)
            pos += count//拷完之后pos要移动
            return count
        }
        //先将buf中的数据读进b
        //大于0，则代表pos到已读下标距离是正，需要先把buff剩余的有效数据都拷到新数组里面先
        if (availables > 0) {
            System.arraycopy(buf!!, pos, b, tempOff, availables)
            //buf!!.copyInto(b, tempOff, pos, availables)
            tempOff += availables//拷入点要发生移动
            pos += availables//pos也要跟着发生移动 pos += availables 实际上就是等于readCount了
        }
        //还需要读取的数据长度 从原inputstream读取
        count = len - tempOff
        val readlen: Int = inputStream.read(b, tempOff, count)
        if (readlen == -1) {//=-1说明已经读到b的末尾了，也就是b已经被填满了，本次读取可以直接返回了。
            return readlen
        }

        //到这里还执行就说明两件事：
        //1.传进来的b把剩余的inputStream数据全装下去了，也就是inputStream已经被全部读完
        //2.buffer目前读到的位置(readCount)，小于这次需要读的数据，但是大小仍未知道是否足够装下剩余内容
        //算一算大小是否还够，这里pos实际上已经等于readCount
        val i = pos + readlen - buf!!.size//buffer已读取位置+剩余内容末尾位置-buffer长度
        if (i > 0) {//如果为正数，则说明buffer长度小于剩余内容长度，需要扩容，反之则说明还够。
            resizeBuf(i)//算到了就拿去扩容
        }
        System.arraycopy(b, tempOff, buf!!, pos, readlen)
        //把剩余内容都写进buffer里面，这样buffer就成为完全体了
        //b.copyInto(buf!!, pos, tempOff, readlen)
        //pos当然要位移
        pos += readlen
        //记录已经读取的总长度，readCount也需要位移
        readCount += readlen
        return readlen


    }

    /**
     * 这是覆写的方法，注意。
     * 重置，其实就是把当前读取位置重置为回溯点就OK了
     */
    override fun reset() {
        pos = markPos
    }

    /**
     * 这是覆写的方法，注意。
     *注意：返回true才能开启mark功能
     */
    override fun markSupported(): Boolean {
        return true
    }

    /**
     * 这是覆写的方法，注意。
     * 进行标记
     * 实际上就是把当前的读点作为回溯点保存起来就ok了
     */
    override fun mark(readlimit: Int) {
        markPos = pos
    }

    /**
     * 关闭流
     */
    override fun close() {
        buf?.apply {
            arrayPool.put(this)
            buf = null
        }
        inputStream.close()
    }

    /**
     * 释放流
     */
    fun release() {
        buf?.apply {
            arrayPool.put(this)
            buf = null
        }
    }
}
package sk.zimen.semestralka.structures.dynamic_hashing

import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IData
import sk.zimen.semestralka.utils.initializeDirectory
import java.io.RandomAccessFile
import kotlin.reflect.KClass

class OverloadHashStructure<K, T : IData<K>>(
    name: String,
    blockFactor: Int,
    clazz: KClass<T>
) : HashStructure<K, T>(
    name,
    "overload_file",
    blockFactor,
    clazz
) {
    override fun initFile(dirName: String, fileName: String) {
        val dir = "data/${dirName}"
        initializeDirectory(dir)
        file = RandomAccessFile("${dir}/${fileName}.bin", "rw")
        file.setLength(blockSize.toLong())
        firstEmptyBlockAddress = file.length()

        //TODO logic when file is not empty at the start
    }
}
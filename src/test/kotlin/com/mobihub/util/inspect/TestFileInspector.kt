package com.mobihub.util.inspect

import com.mobihub.utils.inspect.FileInspectorResult
import com.mobihub.utils.inspect.PseudoFileInspector
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Testfile for the [PseudoFileInspector] class.
 *
 * @author Team-Mobihub
 */
class TestFileInspector {

    @Test
    fun `test valid file with file inspector` () {
        val fileInspector = PseudoFileInspector()
        val file = File("src/test/resources/testfiles/testfile.zip")
        val result = fileInspector.isFileValid(file)
        assertEquals(FileInspectorResult.CLEAN, result)
    }

    @Test
    fun `test invalid file with file inspector` () {
        val fileInspector = PseudoFileInspector()
        val file = File("src/test/resources/testfiles/nonexistentfile.zip")
        val result = fileInspector.isFileValid(file)
        assertEquals(FileInspectorResult.NOT_CLEAN, result)
    }
}
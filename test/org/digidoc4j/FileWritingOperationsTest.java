/* DigiDoc4J library
*
* This software is released under either the GNU Library General Public
* License (see LICENSE.LGPL).
*
* Note that the only valid version of the LGPL license as far as this
* project is concerned is the original GNU Library General Public License
* Version 2.1, February 1999
*/

package org.digidoc4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.digidoc4j.impl.DigiDoc4JTestHelper;
import org.digidoc4j.testutils.RestrictedFileWritingRule;
import org.digidoc4j.testutils.RestrictedFileWritingRule.FileWritingRestrictedException;
import org.digidoc4j.testutils.TestDataBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import eu.europa.esig.dss.MimeType;

public class FileWritingOperationsTest extends DigiDoc4JTestHelper {

  private static final String tslCacheDirectoryPath = System.getProperty("java.io.tmpdir") + File.separator + "dss-cache-tsl" + File.separator;

  @Rule
  public RestrictedFileWritingRule rule = new RestrictedFileWritingRule(new File(tslCacheDirectoryPath).getPath());

  private static Configuration configuration = new Configuration(Configuration.Mode.TEST);

  @Test(expected = FileWritingRestrictedException.class)
  public void writingToFileIsNotAllowed() throws IOException {
    File.createTempFile("test", "test");
  }

  @Test
  public void openingExistingContainer_shouldNotStoreDataFilesOnDisk_byDefault() throws Exception {
    Container container = ContainerBuilder.
        aContainer(ContainerBuilder.BDOC_CONTAINER_TYPE).
        fromExistingFile("testFiles/one_signature.bdoc").
        build();
    TestDataBuilder.signContainer(container);
    container.validate();
    InputStream inputStream = container.saveAsStream();
    assertContainerStream(inputStream);
  }

  @Test
  public void creatingNewContainer_shouldNotStoreDataFilesOnDisk_byDefault() throws Throwable {
    InputStream dataFileInputStream = new ByteArrayInputStream(new byte[]{1, 2, 3});
    File pdfFile = new File("testFiles/special-char-files/dds_acrobat.pdf");
    Container container = ContainerBuilder.
        aContainer(ContainerBuilder.BDOC_CONTAINER_TYPE).
        withDataFile(dataFileInputStream, "test-stream.txt", MimeType.TEXT.getMimeTypeString()).
        withDataFile("testFiles/test.txt", MimeType.TEXT.getMimeTypeString()).
        withDataFile(pdfFile, MimeType.PDF.getMimeTypeString()).
        build();
    TestDataBuilder.signContainer(container);
    container.validate();
    InputStream inputStream = container.saveAsStream();
    assertContainerStream(inputStream);
  }

  @Test
  public void creatingDataFiles_shouldNotStoreDataFilesOnDisk_byDefault() throws Exception {
    DataFile pathDataFile = new DataFile("testFiles/test.txt", MimeType.TEXT.getMimeTypeString());
    DataFile byteDataFile = new DataFile(new byte[]{1, 2, 3}, "byte-file.txt", MimeType.TEXT.getMimeTypeString());
    InputStream dataFileInputStream = new ByteArrayInputStream(new byte[]{1, 2, 3});
    DataFile streamDataFile = new DataFile(dataFileInputStream, "stream-file.txt", MimeType.TEXT.getMimeTypeString());

    Container container = ContainerBuilder.
        aContainer(ContainerBuilder.BDOC_CONTAINER_TYPE).
        withDataFile(pathDataFile).
        withDataFile(byteDataFile).
        withDataFile(streamDataFile).
        build();

    assertEquals(3, container.getDataFiles().size());
    TestDataBuilder.signContainer(container);
    InputStream inputStream = container.saveAsStream();
    assertContainerStream(inputStream);
  }

  @Test(expected = FileWritingRestrictedException.class)
  public void creatingLargeDataFile_shouldStoreFileOnDisk() throws Throwable {
    InputStream dataFileInputStream = new ByteArrayInputStream(new byte[]{1, 2, 3});
    try {
      DataFile dataFile = new LargeDataFile(dataFileInputStream, "stream-file.txt", MimeType.TEXT.getMimeTypeString());
      assertFalse("Did not create a temporary file", true);
    } catch (Exception e) {
      throw e.getCause();
    }
  }

  @Test(expected = FileWritingRestrictedException.class)
  public void openingExistingContainer_withStoringDataFilesOnDisk() throws Exception {
    configuration.setMaxFileSizeCachedInMemoryInMB(0);
    Container container = ContainerBuilder.
        aContainer(ContainerBuilder.BDOC_CONTAINER_TYPE).
        fromExistingFile("testFiles/one_signature.bdoc").
        withConfiguration(configuration).
        build();
    assertEquals(1, container.getDataFiles().size());
  }

  @Test(expected = FileWritingRestrictedException.class)
  public void openingExistingContainer_withLarge2MbFile_shouldStoreDataFilesOnDisk() throws Exception {
    configuration.setMaxFileSizeCachedInMemoryInMB(1);
    Container container = ContainerBuilder.
        aContainer(ContainerBuilder.BDOC_CONTAINER_TYPE).
        fromExistingFile("testFiles/valid-containers/bdoc-ts-with-large-data-file.bdoc").
        withConfiguration(configuration).
        build();
    assertEquals(1, container.getDataFiles().size());
  }

  @Test
  public void openingExistingContainer_withLarge2MbFile_shouldNotStoreDataFilesOnDisk() throws Exception {
    configuration.setMaxFileSizeCachedInMemoryInMB(4);
    Container container = ContainerBuilder.
        aContainer(ContainerBuilder.BDOC_CONTAINER_TYPE).
        fromExistingFile("testFiles/valid-containers/bdoc-ts-with-large-data-file.bdoc").
        withConfiguration(configuration).
        build();
    assertEquals(1, container.getDataFiles().size());
  }

  private void assertContainerStream(InputStream inputStream) throws IOException {
    byte[] containerBytes = IOUtils.toByteArray(inputStream);
    assertTrue(containerBytes.length > 0);
  }
}

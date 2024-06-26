/*
 * Box Platform API
 * [Box Platform](https://box.dev) provides functionality to provide access to content stored within [Box](https://box.com). It provides endpoints for basic manipulation of files and folders, management of users within an enterprise, as well as more complex topics such as legal holds and retention policies.
 *
 * OpenAPI spec version: 2.0.0
 * Contact: devrel@box.com
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package ch.cyberduck.core.box.io.swagger.client.model;

import java.util.Objects;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * FilesUploadSessionsBody
 */


public class FilesUploadSessionsBody {
  @JsonProperty("folder_id")
  private String folderId = null;

  @JsonProperty("file_size")
  private Long fileSize = null;

  @JsonProperty("file_name")
  private String fileName = null;

  public FilesUploadSessionsBody folderId(String folderId) {
    this.folderId = folderId;
    return this;
  }

   /**
   * The ID of the folder to upload the new file to.
   * @return folderId
  **/
  @Schema(example = "0", required = true, description = "The ID of the folder to upload the new file to.")
  public String getFolderId() {
    return folderId;
  }

  public void setFolderId(String folderId) {
    this.folderId = folderId;
  }

  public FilesUploadSessionsBody fileSize(Long fileSize) {
    this.fileSize = fileSize;
    return this;
  }

   /**
   * The total number of bytes of the file to be uploaded
   * @return fileSize
  **/
  @Schema(example = "104857600", required = true, description = "The total number of bytes of the file to be uploaded")
  public Long getFileSize() {
    return fileSize;
  }

  public void setFileSize(Long fileSize) {
    this.fileSize = fileSize;
  }

  public FilesUploadSessionsBody fileName(String fileName) {
    this.fileName = fileName;
    return this;
  }

   /**
   * The name of new file
   * @return fileName
  **/
  @Schema(example = "Project.mov", required = true, description = "The name of new file")
  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FilesUploadSessionsBody filesUploadSessionsBody = (FilesUploadSessionsBody) o;
    return Objects.equals(this.folderId, filesUploadSessionsBody.folderId) &&
        Objects.equals(this.fileSize, filesUploadSessionsBody.fileSize) &&
        Objects.equals(this.fileName, filesUploadSessionsBody.fileName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(folderId, fileSize, fileName);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FilesUploadSessionsBody {\n");
    
    sb.append("    folderId: ").append(toIndentedString(folderId)).append("\n");
    sb.append("    fileSize: ").append(toIndentedString(fileSize)).append("\n");
    sb.append("    fileName: ").append(toIndentedString(fileName)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

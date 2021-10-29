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
 * Defines if this link allows a user to preview and download an item.
 */
@Schema(description = "Defines if this link allows a user to preview and download an item.")

public class FileSharedLinkPermissions {
  @JsonProperty("can_download")
  private Boolean canDownload = null;

  @JsonProperty("can_preview")
  private Boolean canPreview = null;

  public FileSharedLinkPermissions canDownload(Boolean canDownload) {
    this.canDownload = canDownload;
    return this;
  }

   /**
   * Defines if the shared link allows for the item to be downloaded. For shared links on folders, this also applies to any items in the folder.  This value can be set to &#x60;true&#x60; when the effective access level is set to &#x60;open&#x60; or &#x60;company&#x60;, not &#x60;collaborators&#x60;.
   * @return canDownload
  **/
  @Schema(example = "true", description = "Defines if the shared link allows for the item to be downloaded. For shared links on folders, this also applies to any items in the folder.  This value can be set to `true` when the effective access level is set to `open` or `company`, not `collaborators`.")
  public Boolean isCanDownload() {
    return canDownload;
  }

  public void setCanDownload(Boolean canDownload) {
    this.canDownload = canDownload;
  }

  public FileSharedLinkPermissions canPreview(Boolean canPreview) {
    this.canPreview = canPreview;
    return this;
  }

   /**
   * Defines if the shared link allows for the item to be previewed.  This value is always &#x60;true&#x60;. For shared links on folders this also applies to any items in the folder.
   * @return canPreview
  **/
  @Schema(example = "true", description = "Defines if the shared link allows for the item to be previewed.  This value is always `true`. For shared links on folders this also applies to any items in the folder.")
  public Boolean isCanPreview() {
    return canPreview;
  }

  public void setCanPreview(Boolean canPreview) {
    this.canPreview = canPreview;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FileSharedLinkPermissions fileSharedLinkPermissions = (FileSharedLinkPermissions) o;
    return Objects.equals(this.canDownload, fileSharedLinkPermissions.canDownload) &&
        Objects.equals(this.canPreview, fileSharedLinkPermissions.canPreview);
  }

  @Override
  public int hashCode() {
    return Objects.hash(canDownload, canPreview);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FileSharedLinkPermissions {\n");
    
    sb.append("    canDownload: ").append(toIndentedString(canDownload)).append("\n");
    sb.append("    canPreview: ").append(toIndentedString(canPreview)).append("\n");
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

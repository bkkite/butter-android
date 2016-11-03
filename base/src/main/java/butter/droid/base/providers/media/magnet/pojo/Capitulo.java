package butter.droid.base.providers.media.magnet.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Capitulo {
    @SerializedName("overviewcapitul")
    @Expose
    public String overviewcapitul;
    @SerializedName("numerocapitul")
    @Expose
    public String numerocapitul;
    @SerializedName("infocapitul")
    @Expose
    public String infocapitul;
    @SerializedName("nomcapitul")
    @Expose
    public String nomcapitul;
    @SerializedName("links")
    @Expose
    public Links links;
}

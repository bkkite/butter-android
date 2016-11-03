package butter.droid.base.providers.media.magnet.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class M720 {

    @SerializedName("magnet")
    @Expose
    public String magnet;
    @SerializedName("quality")
    @Expose
    public String quality;
    @SerializedName("peers")
    @Expose
    public Integer peers;
    @SerializedName("hash")
    @Expose
    public String hash;

}

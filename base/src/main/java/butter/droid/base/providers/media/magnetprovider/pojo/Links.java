package butter.droid.base.providers.media.magnetprovider.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Links {

    @SerializedName("magnet")
    @Expose
    public String magnet;
    @SerializedName("calitat")
    @Expose
    public String calitat;
    @SerializedName("hash")
    @Expose
    public String hash;

}

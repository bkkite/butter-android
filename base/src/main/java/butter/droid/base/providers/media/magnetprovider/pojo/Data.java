package butter.droid.base.providers.media.magnetprovider.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Data {

    @SerializedName("sec")
    @Expose
    public Integer sec;
    @SerializedName("usec")
    @Expose
    public Integer usec;

}

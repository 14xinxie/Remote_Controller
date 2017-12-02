package com.example.xinxie.remote_conroller.gson;

import com.google.gson.annotations.SerializedName;

public class Now {

    @SerializedName("tmp")
    public String temperature;

    @SerializedName("cond")
    public More more;

    public class More {

        @SerializedName("txt")
        public String info;

        @SerializedName("code")
        public String pictureId;

    }

}

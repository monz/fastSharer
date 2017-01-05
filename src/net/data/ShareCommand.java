package net.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ShareCommand<T> {

    public enum ShareCommandType {
        PUSH_SHARE_LIST,
        DOWNLOAD_REQUEST,
        DOWNLOAD_REQUEST_RESULT,
        DOWNLOAD_FILE,
        CHUNK_INFO_REQUEST,
        CHUNK_INFO_REQUEST_RESULT,
    }

    @Expose private ShareCommandType cmd;
    @Expose private List<T> data;

    public ShareCommand(ShareCommandType cmd) {
        this.cmd = cmd;
        this.data = new ArrayList<>();
    }

    public ShareCommandType getCmd() {
        return cmd;
    }

    public List<T> getData() {
        return data;
    }

    public void addData(T t) {
        this.data.add(t);
    }

    public String serialize(TypeAdapter typeAdapter) {
        Type type = new TypeToken<ShareCommand<T>>() {}.getType();

        GsonBuilder builder = new GsonBuilder();
        // set custom serializer if available
        if (typeAdapter != null) {
            builder.registerTypeAdapter(type, typeAdapter);
        }

        builder.excludeFieldsWithoutExposeAnnotation();
        Gson gson = builder.create();

        return gson.toJson(this, type);
    }

    @SuppressWarnings("unchecked")
    public static ShareCommand deserialize(String msg, Type type) {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();

        return gson.fromJson(msg, type);
    }
}
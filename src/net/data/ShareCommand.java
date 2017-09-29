/*
 * Copyright (c) 2017. Markus Monz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
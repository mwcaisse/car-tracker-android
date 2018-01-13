package com.ricex.cartracker.common.util;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ricex.cartracker.common.entity.LogType;

public class JsonLogTypeSerializer implements JsonSerializer<LogType>, JsonDeserializer<LogType> {

	@Override
	public LogType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		return LogType.fromId(json.getAsInt());
	}

	@Override
	public JsonElement serialize(LogType src, Type typeOfSrc, JsonSerializationContext context) {
		return new JsonPrimitive(src.getId());
	}

}

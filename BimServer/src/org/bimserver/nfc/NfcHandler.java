package org.bimserver.nfc;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.bimserver.BimServer;
import org.bimserver.plugins.serializers.SerializerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

public class NfcHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(NfcHandler.class);
	private final BimServer bimServer;
	Map<String, Map<String, String>> nfcReadersMap = new HashMap<String, Map<String, String>>();
	Map<String, Map<String, String>> nfcTagsMap = new HashMap<String, Map<String, String>>();
	String readersFilePath;
	String tagsFilePath;
	
	public NfcHandler(BimServer bimServer) throws IOException {
		this.bimServer = bimServer;
		readFromFile();
	}

	public void handleNfcMethods(JsonObject request, String jsonToken, HttpServletRequest httpRequest, JsonWriter writer) throws IOException, SerializerException{
		LOGGER.info("NfcHandler | handleNfcMethods");
		String methodName = request.get("method").getAsString();
		if(methodName.equals("saveNfcData")){
			saveNfcData(request, writer);
		} else if(methodName.equals("getNfcTagData")){
			getNfcTagData(writer);
		} else if(methodName.equals("assignTagIdToIfcNode")){
			assignTagIdToIfcNode(request, writer);
		}
	}
	
	private void saveNfcData(JsonObject request, JsonWriter writer) throws IOException{
		LOGGER.info("NfcHandler | handleNfcMethods | saveNfcData");
		JsonObject parameters = request.getAsJsonObject("parameters");
		String readerId = parameters.get("readerId").getAsString();
		String readerName = parameters.get("readerName").getAsString();
		String readerLocation = parameters.get("readerLocation").getAsString();
		String tagId = parameters.get("tagId").getAsString();
		
		Map<String, String> currentReaderEntry = this.nfcReadersMap.get(readerId);
		if(currentReaderEntry == null) {
			currentReaderEntry = new HashMap<String, String>();
		}
		currentReaderEntry.put("readerId", readerId);
		currentReaderEntry.put("readerName", readerName);
		currentReaderEntry.put("readerLocation", readerLocation);
		this.nfcReadersMap.put(readerId, currentReaderEntry);
		
		Map<String, String> currentTagEntry = this.nfcTagsMap.get(tagId);
		if(currentTagEntry == null) {
			currentTagEntry = new HashMap<String, String>();
		}
		currentTagEntry.put("tagId", tagId);
		currentTagEntry.put("ifcNodeId", "");
		currentTagEntry.put("LatestLocation", readerLocation);
		String trackedLocations = currentTagEntry.get("trackedLocations");
		if((trackedLocations == null) || trackedLocations.isEmpty()){
			trackedLocations = readerLocation;
		} else if (!trackedLocations.contains(readerLocation)){
			trackedLocations += "|" + readerLocation;
		}
		currentTagEntry.put("trackedLocations", trackedLocations);
		this.nfcTagsMap.put(tagId, currentTagEntry);
		
		
		
		writeToNfcReaderFile();
		writeToNfcTagFile();
		
		
		writer.beginObject();
		writer.name("result");
		
		writer.beginObject();
		writer.name("readerId").value(readerId);
		writer.name("tagId").value(tagId);
		writer.endObject();
		writer.endObject();
	}
	
	private void getNfcTagData(JsonWriter writer) throws IOException{
		LOGGER.info("NfcHandler | handleNfcMethods | getNfcTagData");
		writer.beginObject();
		writer.name("result");
		writer.beginArray();
		for (Map.Entry<String, Map<String, String>> entry : this.nfcTagsMap.entrySet()){
			Map<String, String> currentTagMap = entry.getValue();
			writer.beginObject();
			writer.name("tagId").value(currentTagMap.get("tagId"));
			writer.name("ifcNodeId").value(currentTagMap.get("ifcNodeId"));
			writer.name("LatestLocation").value(currentTagMap.get("LatestLocation"));
			writer.name("trackedLocations").value(currentTagMap.get("trackedLocations"));
			writer.endObject();
		}
		writer.endArray();
		writer.endObject();
	}
	
	private void assignTagIdToIfcNode(JsonObject request, JsonWriter writer) throws IOException {
		LOGGER.info("NfcHandler | handleNfcMethods | assignTagIdToIfcNode");
		
		JsonObject parameters = request.getAsJsonObject("parameters");
		String ifcNodeId = parameters.get("ifcNodeId").getAsString();
		String tagId = parameters.get("tagId").getAsString();
		
		Map<String, String> currentTagMap = this.nfcTagsMap.get(tagId);
		currentTagMap.put("ifcNodeId", ifcNodeId);
		writeToNfcTagFile();
		
		writer.beginObject();
		writer.name("result");
		writer.beginObject();
		writer.name("ifcNodeId").value(ifcNodeId);
		writer.name("tagId").value(tagId);
		writer.endObject();
		writer.endObject();
		
	}
	
	@SuppressWarnings("unchecked")
	private void readFromFile() throws IOException{
		File nfcDataDir = new File(this.bimServer.getHomeDir().getAbsolutePath() + "/NfcData");
		if(!nfcDataDir.exists()){
			nfcDataDir.mkdir();
		}
		
		this.readersFilePath = nfcDataDir.getAbsolutePath() + "/nfcReaders.txt";
		File readersFile = new File(this.readersFilePath);
		if(!readersFile.exists()){
			readersFile.createNewFile();
		}
		this.tagsFilePath = nfcDataDir.getAbsolutePath() + "/nfcTags.txt";
		File tagsFile = new File(this.tagsFilePath);
		if(!tagsFile.exists()){
			tagsFile.createNewFile();
		}
		
		List<String> list = FileUtils.readLines(new File(this.readersFilePath), "UTF-8");
		for(String line : list){
			Map<String, String> tempMap = new HashMap<String, String>();
			if(line.startsWith("//")) continue;
			String[] lineArr = line.split(",");
			tempMap.put("readerId", lineArr[0]);
			tempMap.put("readerName", lineArr[1]);
			tempMap.put("readerLocation", lineArr[2]);
			
			this.nfcReadersMap.put(tempMap.get("readerId"), tempMap);
		}
		
		list = FileUtils.readLines(new File(this.tagsFilePath), "UTF-8");
		for(String line : list){
			Map<String, String> tempMap = new HashMap<String, String>();
			if(line.startsWith("//")) continue;
			String[] lineArr = line.split(",");
			tempMap.put("tagId", lineArr[0]);
			tempMap.put("ifcNodeId", lineArr[1]);
			tempMap.put("LatestLocation", lineArr[2]);
			tempMap.put("trackedLocations", lineArr[3]);
			
			this.nfcTagsMap.put(tempMap.get("tagId"), tempMap);
		}
	}
	
	private void writeToNfcReaderFile() throws IOException{
		String content = "//readerId, readerName, readerLocation\n";
		for (Map.Entry<String, Map<String, String>> entry : this.nfcReadersMap.entrySet()){
			Map<String, String> tempMap = entry.getValue();
			content += tempMap.get("readerId");
			content += "," + tempMap.get("readerName");
			content += "," + tempMap.get("readerLocation");
			content += "\n";
		}
		FileUtils.writeStringToFile(new File(this.readersFilePath), content, "UTF-8");
	}
	
	private void writeToNfcTagFile() throws IOException{
		String content = "//tagId, ifcNodeId, latestLocation, trackedLocations\n";
		for (Map.Entry<String, Map<String, String>> entry : this.nfcTagsMap.entrySet()){
			Map<String, String> tempMap = entry.getValue();
			content += tempMap.get("tagId");
			content += "," + tempMap.get("ifcNodeId");
			content += "," + tempMap.get("LatestLocation");
			content += "," + tempMap.get("trackedLocations");
			content += "\n";
		}
		FileUtils.writeStringToFile(new File(this.tagsFilePath), content, "UTF-8");
	}
	
}

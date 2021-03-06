package org.bimserver.nfc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
		} else if(methodName.equals("getNfcReadersData")){
			getNfcReadersData(writer);
		} else if(methodName.equals("assignTagIdToIfcNode")){
			assignTagIdToIfcNode(request, writer);
		} else if(methodName.equals("getNfcTagDataByIfcNodeId")){
			getNfcTagDataByIfcNodeId(request, writer);
		} else if(methodName.equals("registerNfcReader")){
			registerNfcReader(request, writer);
		} else if(methodName.equals("registerNfcTag")){
			registerNfcTag(request, writer);
		}
	}
	
	private void saveNfcData(JsonObject request, JsonWriter writer) throws IOException{
		LOGGER.info("NfcHandler | handleNfcMethods | saveNfcData");
		JsonObject parameters = request.getAsJsonObject("parameters");
		String readerId = parameters.get("readerId").getAsString();
		String readerName = parameters.get("readerName").getAsString();
		String readerLocation = parameters.get("readerLocation").getAsString();
		String nfcTagId = parameters.get("nfcTagId").getAsString();
		
		Map<String, String> currentReaderEntry = this.nfcReadersMap.get(readerId);
		if(currentReaderEntry == null) {
			//this should never occur.
			currentReaderEntry = new HashMap<String, String>();
		}
		//currentReaderEntry.put("readerId", readerId);
		currentReaderEntry.put("readerName", readerName);
		currentReaderEntry.put("readerLocation", readerLocation);
		this.nfcReadersMap.put(readerId, currentReaderEntry);
		
		Map<String, String> currentTagEntry = this.nfcTagsMap.get(nfcTagId);
		if(currentTagEntry == null) {
			currentTagEntry = new HashMap<String, String>();
		}
		currentTagEntry.put("nfcTagId", nfcTagId);
		if(currentTagEntry.get("ifcNodeId") == null) currentTagEntry.put("ifcNodeId", "");
		currentTagEntry.put("latestLocation", readerLocation);
		String trackedLocations = currentTagEntry.get("trackedLocations");
		if((trackedLocations == null) || trackedLocations.isEmpty()){
			trackedLocations = readerLocation;
		} else if (!trackedLocations.contains(readerLocation)){
			trackedLocations += "|" + readerLocation;
		}
		currentTagEntry.put("trackedLocations", trackedLocations);
		this.nfcTagsMap.put(nfcTagId, currentTagEntry);
		
		
		
		writeToNfcReaderFile();
		writeToNfcTagFile();
		
		
		writer.beginObject();
		writer.name("result");
		
		writer.beginObject();
		writer.name("readerId").value(readerId);
		writer.name("nfcTagId").value(nfcTagId);
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
			writer.name("nfcTagId").value(currentTagMap.get("nfcTagId"));
			writer.name("ifcNodeId").value(currentTagMap.get("ifcNodeId"));
			writer.name("latestLocation").value(currentTagMap.get("latestLocation"));
			writer.name("trackedLocations").value(currentTagMap.get("trackedLocations"));
			writer.endObject();
		}
		writer.endArray();
		writer.endObject();
	}
	
	private void getNfcReadersData(JsonWriter writer) throws IOException{
		LOGGER.info("NfcHandler | handleNfcMethods | getNfcReadersData");
		writer.beginObject();
		writer.name("result");
		writer.beginArray();
		for (Map.Entry<String, Map<String, String>> entry : this.nfcReadersMap.entrySet()){
			Map<String, String> currentTagMap = entry.getValue();
			writer.beginObject();
			writer.name("readerId").value(currentTagMap.get("readerId"));
			writer.name("readerName").value(currentTagMap.get("readerName"));
			writer.name("readerLocation").value(currentTagMap.get("readerLocation"));
			writer.endObject();
		}
		writer.endArray();
		writer.endObject();
	}
	
	private void getNfcTagDataByIfcNodeId(JsonObject request, JsonWriter writer) throws IOException{
		LOGGER.info("NfcHandler | handleNfcMethods | getNfcTagDataByIfcNodeId");
		JsonObject parameters = request.getAsJsonObject("parameters");
		String ifcNodeId = parameters.get("ifcNodeId").getAsString();
		
		writer.beginObject();
		writer.name("result");
		writer.beginObject();
		for (Map.Entry<String, Map<String, String>> entry : this.nfcTagsMap.entrySet()){
			Map<String, String> currentTagMap = entry.getValue();
			if(currentTagMap.get("ifcNodeId").equals(ifcNodeId)){
				writer.name("nfcTagId").value(currentTagMap.get("nfcTagId"));
				writer.name("ifcNodeId").value(currentTagMap.get("ifcNodeId"));
				writer.name("latestLocation").value(currentTagMap.get("latestLocation"));
				writer.name("trackedLocations").value(currentTagMap.get("trackedLocations"));
			}
		}
		writer.endObject();
		writer.endObject();
	}
	
	private void assignTagIdToIfcNode(JsonObject request, JsonWriter writer) throws IOException {
		LOGGER.info("NfcHandler | handleNfcMethods | assignTagIdToIfcNode");
		
		JsonObject parameters = request.getAsJsonObject("parameters");
		String ifcNodeId = parameters.get("ifcNodeId").getAsString();
		String nfcTagId = parameters.get("nfcTagId").getAsString();
		
		for (Map.Entry<String, Map<String, String>> entry : this.nfcTagsMap.entrySet()){
			Map<String, String> currentTagMap = entry.getValue();
			if(currentTagMap.get("ifcNodeId").equals(ifcNodeId)){
				currentTagMap.put("ifcNodeId", "");
				currentTagMap.put("latestLocation", "");
				currentTagMap.put("trackedLocations", "");
			}
		}
		
		Map<String, String> currentTagMap = this.nfcTagsMap.get(nfcTagId);
		currentTagMap.put("ifcNodeId", ifcNodeId);
		currentTagMap.put("latestLocation", "");
		currentTagMap.put("trackedLocations", "");
		writeToNfcTagFile();
		
		writer.beginObject();
		writer.name("result");
		writer.beginObject();
		writer.name("ifcNodeId").value(ifcNodeId);
		writer.name("nfcTagId").value(nfcTagId);
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
			String[] lineArr = line.split(",", -1);
			tempMap.put("readerId", lineArr[0]);
			tempMap.put("readerName", lineArr[1]);
			tempMap.put("readerLocation", lineArr[2]);
			
			this.nfcReadersMap.put(tempMap.get("readerId"), tempMap);
		}
		
		list = FileUtils.readLines(new File(this.tagsFilePath), "UTF-8");
		for(String line : list){
			Map<String, String> tempMap = new HashMap<String, String>();
			if(line.startsWith("//")) continue;
			String[] lineArr = line.split(",", -1);
			tempMap.put("nfcTagId", lineArr[0]);
			tempMap.put("ifcNodeId", lineArr[1]);
			tempMap.put("latestLocation", lineArr[2]);
			tempMap.put("trackedLocations", lineArr[3]);
			
			this.nfcTagsMap.put(tempMap.get("nfcTagId"), tempMap);
		}
	}
	
	private void writeToNfcReaderFile() throws IOException{
		String content = "//readerId, readerName, readerLocation";
		for (Map.Entry<String, Map<String, String>> entry : this.nfcReadersMap.entrySet()){
			Map<String, String> tempMap = entry.getValue();
			content += "\n";
			content += tempMap.get("readerId");
			content += "," + tempMap.get("readerName");
			content += "," + tempMap.get("readerLocation");
		}
		FileUtils.writeStringToFile(new File(this.readersFilePath), content, "UTF-8");
	}
	
	private void writeToNfcTagFile() throws IOException{
		String content = "//nfcTagId, ifcNodeId, latestLocation, trackedLocations";
		for (Map.Entry<String, Map<String, String>> entry : this.nfcTagsMap.entrySet()){
			Map<String, String> tempMap = entry.getValue();
			content += "\n";
			content += tempMap.get("nfcTagId");
			content += "," + tempMap.get("ifcNodeId");
			content += "," + tempMap.get("latestLocation");
			content += "," + tempMap.get("trackedLocations");
			
		}
		FileUtils.writeStringToFile(new File(this.tagsFilePath), content, "UTF-8");
	}
	
	private void registerNfcReader(JsonObject request, JsonWriter writer) throws IOException{
		LOGGER.info("NfcHandler | handleNfcMethods | registerNfcReader");
		JsonObject parameters = request.getAsJsonObject("parameters");
		String readerName = parameters.get("readerName").getAsString();
		String readerLocation = parameters.get("readerLocation").getAsString();
		String readerId = generateReaderId();
		
		Map<String, String> newReaderMap = new HashMap<String, String>();
		newReaderMap.put("readerId", readerId);
		newReaderMap.put("readerName", readerName);
		newReaderMap.put("readerLocation", readerLocation);
		this.nfcReadersMap.put(readerId, newReaderMap);
		writeToNfcReaderFile();
		
		writer.beginObject();
		writer.name("result");
		writer.beginObject();
		writer.name("readerId").value(readerId);
		writer.name("readerName").value(readerName);
		writer.name("readerLocation").value(readerLocation);
		writer.endObject();
		writer.endObject();
	}
	
	private void registerNfcTag(JsonObject request, JsonWriter writer) throws IOException{
		LOGGER.info("NfcHandler | handleNfcMethods | registerNfcTag");
		String nfcTagId = generateTagId();
		
		Map<String, String>  newTagMap = new HashMap<String, String>();
		newTagMap.put("nfcTagId", nfcTagId);
		newTagMap.put("ifcNodeId", "");
		newTagMap.put("latestLocation", "");
		newTagMap.put("trackedLocations", "");
		this.nfcTagsMap.put(nfcTagId, newTagMap);
		writeToNfcTagFile();
		
		writer.beginObject();
		writer.name("result");
		writer.beginObject();
		writer.name("nfcTagId").value(nfcTagId);
		writer.endObject();
		writer.endObject();
	}
	
	private String generateReaderId(){
		String readerId;
		Integer randId;
		ArrayList<String> existingIds = new ArrayList<String>();
		for (Map.Entry<String, Map<String, String>> entry : this.nfcReadersMap.entrySet()){
			Map<String, String> tempMap = entry.getValue();
			existingIds.add(tempMap.get("readerId"));
			
		}
		do {
			randId = (int) Math.abs(Math.random() * 100000);
			readerId = "r" + randId.toString();
		} while(existingIds.contains(readerId));
		
		return readerId.toString();
	}
	
	private String generateTagId(){
		String tagId;
		Integer randId;
		ArrayList<String> existingIds = new ArrayList<String>();
		for (Map.Entry<String, Map<String, String>> entry : this.nfcTagsMap.entrySet()){
			Map<String, String> tempMap = entry.getValue();
			existingIds.add(tempMap.get("nfcTagId"));
			
		}
		do {
			randId = (int) Math.abs(Math.random() * 100000);
			tagId = "t" + randId.toString();
		} while(existingIds.contains(tagId));
		
		return tagId.toString();
	}
	
}

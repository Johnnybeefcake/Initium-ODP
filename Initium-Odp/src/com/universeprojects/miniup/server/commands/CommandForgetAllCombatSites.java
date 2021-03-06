package com.universeprojects.miniup.server.commands;


import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.universeprojects.cacheddatastore.CachedDatastoreService;
import com.universeprojects.cacheddatastore.CachedEntity;
import com.universeprojects.miniup.server.ODPDBAccess;
import com.universeprojects.miniup.server.commands.framework.Command;
import com.universeprojects.miniup.server.commands.framework.UserErrorMessage;
import com.universeprojects.miniup.server.services.CombatService;
import com.universeprojects.miniup.server.services.MainPageUpdateService;

/**
 * Allows the player to attempt to forget all combat sites at their location.
 * 
 * @author tacobowl8
 * 
 */
public class CommandForgetAllCombatSites extends Command {
	
	private static final long MAX_MILLISECONDS_TO_SPEND = 10000;
	
	public CommandForgetAllCombatSites(ODPDBAccess db, HttpServletRequest request, HttpServletResponse response) {
		super(db, request, response);
	}
	
	public void run(Map<String, String> parameters) throws UserErrorMessage {
		long startTime = System.currentTimeMillis();
		int numberOfSitesForgotten = 0;
		ODPDBAccess db = getDB();
		CachedDatastoreService ds = getDS();
		ds.beginBulkWriteMode();

		CachedEntity character = db.getCurrentCharacter();
		List<Long> forgettableCombatSiteList = tryParseStringToLongList(parameters, "forgettableCombatSiteArray", ",");
		//The location the command is being called from
		Key characterLocationKey = (Key)character.getProperty("locationKey");
		try {
			for(Long forgettableCombatSite : forgettableCombatSiteList) {
				if(System.currentTimeMillis() - startTime >= MAX_MILLISECONDS_TO_SPEND)
					throw new UserErrorMessage("The bulk forgetting of sites has stopped due to it taking a while.  A total of "+numberOfSitesForgotten+" sites were forgotten.");
				db.doDeleteCombatSite(ds, character, KeyFactory.createKey("Location", forgettableCombatSite));
				numberOfSitesForgotten++;
			}
		} catch (UserErrorMessage e) {
			throw e;
		} finally {
			ds.commitBulkWrite();
			MainPageUpdateService mpus = new MainPageUpdateService(db, db.getCurrentUser(), character, db.getLocationById(characterLocationKey.getId()), this);
			mpus.updateButtonList(new CombatService(db));
		}
	}
}

package com.widen.valet.examples;

import com.widen.valet.*;
import com.widen.valet.util.NameQueryByRoute53APIService;
import com.widen.valet.util.NameQueryService;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple example usage of using Valet API to create and update DNS zones in AWS Route53
 */
public class ValetExample
{
	private static final String AWS_ACCESS_KEY = "";

	private static final String AWS_SECRET_KEY = "";

	public static void main(String[] args)
	{
		new ValetExample().run();
	}

	private void run()
	{
		String domain = "foodomain.com.";

		String resource = String.format("www.%s", domain);

		String resourceValue = "127.0.0.2";

		//Route53Driver is the API abstraction for accessing Route53
		Route53Driver driver = new Route53Driver(AWS_ACCESS_KEY, AWS_SECRET_KEY);

		Zone zone = driver.zoneDetailsForDomain(domain);

		if (zone.equals(Zone.NON_EXISTENT_ZONE))
		{
			//create the Route53 zone if it does not exist
			//WARNING: Route53 allows you to create multiple zones using the same domain

			ZoneChangeStatus createStatus = driver.createZone(domain, "Create zone " + domain);

			//you should not modify zones that are not INSYNC
			driver.waitForSync(createStatus);

			zone = driver.zoneDetails(createStatus.zoneId);
		}

		System.out.println("zone: " + zone);

		//Simple query service to search for existing resources within the zone
		NameQueryService queryService = new NameQueryByRoute53APIService(driver, zone);

		NameQueryService.LookupRecord lookup = queryService.lookup(resource, RecordType.A);

		//Holds the commands to run within the update transaction
		List<ZoneUpdateAction> actions = new ArrayList<ZoneUpdateAction>();

		if (lookup.exists)
		{
			//if the resource exists it must be deleted within the update transaction
			ZoneUpdateAction delete = ZoneUpdateAction.deleteAction(resource, RecordType.A, 600, lookup.getFirstValue());
			actions.add(delete);
		}

		ZoneUpdateAction create = ZoneUpdateAction.createAction(resource, RecordType.A, 600, resourceValue);
		actions.add(create);

		//Update zone will throw a ValetException if Route53 rejects the transaction block
		ZoneChangeStatus updateChangeStatus = driver.updateZone(zone, "Update WWW record", actions);

		driver.waitForSync(updateChangeStatus);

		List<ZoneResource> zoneResources = driver.listZoneRecords(zone);

		System.out.println("Update complete...");

		for (ZoneResource zoneResource : zoneResources)
		{
			System.out.println(zoneResource);
		}
	}

}

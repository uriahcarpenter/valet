package com.widen.valet.examples;

import com.widen.valet.RecordType;
import com.widen.valet.Route53Driver;
import com.widen.valet.Zone;
import com.widen.valet.util.NameQueryByRoute53APIService;
import com.widen.valet.util.NameQueryService;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class ZoneSummary
{
	private static final String AWS_ACCESS_KEY = "";

	private static final String AWS_SECRET_KEY = "";

	private Route53Driver driver;

	private List<ZoneSummaryData> out = new ArrayList<ZoneSummaryData>();

	public static void main(String[] args)
	{
		new ZoneSummary().run();
	}

	public ZoneSummary()
	{
		driver = new Route53Driver(AWS_ACCESS_KEY, AWS_SECRET_KEY);
	}

	private void run()
	{
		List<Zone> zones = driver.listZones();

		Collections.sort(zones);

		for (Zone z : zones)
		{
			System.out.println(z.name + "..");

			Zone zoneDetail = driver.zoneDetails(z);

			out.add(processZone(zoneDetail));
		}

		System.out.println(new ZoneSummaryData().toStringHeader());

		System.out.println(StringUtils.join(out, "\n"));
	}

	private ZoneSummaryData processZone(Zone z)
	{
		NameQueryService queryService = new NameQueryByRoute53APIService(driver, z);

		ZoneSummaryData data = new ZoneSummaryData();

		data.zoneId = z.zoneId;

		data.domain = z.name;

		data.apexRecord = queryService.lookup(z.name, RecordType.A).getFirstValue();

		data.wwwRecord = queryService.lookup("www." + z.name, RecordType.A).getFirstValue();

		if (data.wwwRecord.isEmpty())
		{
			data.wwwRecord = queryService.lookup("www." + z.name, RecordType.CNAME).getFirstValue();
		}

		data.nsRecords.addAll(z.nameServers);

		data.mxRecords = queryService.lookup(z.name, RecordType.MX).values;

		return data;
	}

	private class ZoneSummaryData
	{
		String zoneId;
		String domain;
		String apexRecord;
		String wwwRecord;
		List<String> nsRecords = new ArrayList<String>();
		List<String> mxRecords = new ArrayList<String>();

		@Override
		public String toString()
		{
			String t = "\t";

			StringBuilder sb = new StringBuilder();

			sb.append(domain + t);
			sb.append(zoneId + t);
			sb.append(apexRecord + t);
			sb.append(wwwRecord + t);

			Collections.sort(nsRecords, new Comparator<String>()
			{
				@Override
				public int compare(String s1, String s2)
				{
					return StringUtils.reverse(s1).compareTo(StringUtils.reverse(s2));
				}
			});

			sb.append(StringUtils.join(nsRecords, t) + t);
			sb.append(StringUtils.join(mxRecords, ", "));

			return sb.toString();
		}

		public String toStringHeader()
		{
			String t = "\t";

			StringBuilder sb = new StringBuilder();

			sb.append("Domain" + t);
			sb.append("Zone Id" + t);
			sb.append("Apex Record" + t);
			sb.append("WWW Record" + t);
			sb.append(StringUtils.join(Arrays.asList("Name Server 1", "Name Server 2", "Name Server 3", "Name Server 4"), t) + t);
			sb.append("MX Records");

			return sb.toString();

		}
	}
}

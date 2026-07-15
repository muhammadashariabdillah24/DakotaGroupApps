<!--#include file="../../connection.asp" -->

<%
response.Buffer=true
Response.ContentType = "application/json;charset=utf-8"

dim latlon_dli
dim latlon_dli_cmd
'dim latlon_dbs
'dim latlon_dbs_cmd

set latlon_dli_cmd = server.CreateObject("adodb.command")
latlon_dli_cmd.activeConnection = MM_Cargo_string

'set latlon_dbs_cmd = server.CreateObject("adodb.command")
'latlon_dbs_cmd.activeConnection = MM_DBS_STRING

latlon_dli_cmd.commandText = "SELECT GLB_M_Agen.Agen_ID, GLB_M_Agen.Agen_Nama, ISNULL(GLB_M_Agen.Agen_Lat,'') AS Agen_Lat, ISNULL(GLB_M_Agen.Agen_Long,'') AS Agen_Long, isnull(GLB_M_Agen.Agen_md5,'') AS Agen_md5, isnull(GLB_M_AgenRangeGPS.AgenG_Range,100) AS AgenG_Range FROM GLB_M_Agen LEFT OUTER JOIN GLB_M_AgenRangeGPS ON GLB_M_Agen.Agen_ID = GLB_M_AgenRangeGPS.AgenG_AgenID WHERE (GLB_M_Agen.Agen_AktifYN = 'Y') AND (GLB_M_Agen.Agen_Nama NOT LIKE '%XXX%') ORDER BY GLB_M_Agen.Agen_Nama"
set latlon_dli = latlon_dli_cmd.execute

'latlon_dbs_cmd.commandText = "SELECT GLB_M_Agen.Agen_ID, GLB_M_Agen.Agen_Nama, ISNULL(GLB_M_Agen.Agen_Lat,'') AS Agen_Lat, ISNULL(GLB_M_Agen.Agen_Long,'') AS Agen_Long, isnull(GLB_M_Agen.Agen_md5,'') AS Agen_md5, isnull(GLB_M_AgenRangeGPS.AgenG_Range,100) AS AgenG_Range FROM GLB_M_Agen LEFT OUTER JOIN GLB_M_AgenRangeGPS ON GLB_M_Agen.Agen_ID = GLB_M_AgenRangeGPS.AgenG_AgenID WHERE (GLB_M_Agen.Agen_AktifYN = 'Y') AND (GLB_M_Agen.Agen_Nama NOT LIKE '%XXX%') ORDER BY GLB_M_Agen.Agen_Nama"
'set latlon_dbs = latlon_dbs_cmd.execute

%>
[
  


<%do while not latlon_dli.eof%>
	{
	"KodeAgen":<%=trim(latlon_dli("agen_ID"))%>,
	"NamaAgen":"<%=trim(latlon_dli("agen_nama"))%>",
	"AgenLat":"<%=trim(replace(latlon_dli("Agen_Lat"),"	",""))%>",
	"AgenLon":"<%=trim(replace(latlon_dli("Agen_Long"),"	",""))%>",
	"AgenMD5":"<%=trim(replace(latlon_dli("Agen_MD5"),"	",""))%>",
	"AgenRange":"<%=trim(replace(latlon_dli("AgenG_Range"),"	",""))%>"
	}

<%
				
latlon_dli.movenext
if latlon_dli.eof = false then
	response.write ","
end if
loop


%>
]
<!--#include file="../../connection.asp" -->   
<%

	Dim cbid, nourut, cek1, cek1_cmd, cek2, cek2_cmd, supir1, supir2, insorupdate, insorupdate_cmd
	set cek1_cmd = server.CreateObject("ADODB.command")
	cek1_cmd.activeConnection = MM_Cargo_String
	set cek2_cmd = server.CreateObject("ADODB.command")
	cek2_cmd.activeConnection = MM_Cargo_String
	set insorupdate_cmd = server.CreateObject("ADODB.command")
	insorupdate_cmd.activeConnection = MM_Cargo_String


	If Request.TotalBytes > 0 Then
		Dim lngBytesCount, post
		lngBytesCount = Request.TotalBytes
		post = BytesToStr(Request.BinaryRead(lngBytesCount))
		Response.ContentType = "text/plain"
		nilaiB = trim(post)
	End If


	Function BytesToStr(bytes)
	Dim Stream
	Set Stream = Server.CreateObject("Adodb.Stream")
		Stream.Type = 1 'adTypeBinary
		Stream.Open
		Stream.Write bytes
		Stream.Position = 0
		Stream.Type = 2 'adTypeText
		Stream.Charset = "iso-8859-1"
		BytesToStr = Stream.ReadText
		Stream.Close
	Set Stream = Nothing
	End Function

	Dim b

	nilaiB = trim(nilaiB)
	nilaiB = replace(nilaiB,"{","")
	nilaiB = replace(nilaiB,"}","")
	nilaiB = replace(nilaiB,"params","")
	nilaiB = replace(nilaiB,"""","")
	nilaiB = replace(nilaiB,":","")
	nilaiB = replace(nilaiB,"\n", vbCrLf)

	b = trim(nilaiB)

	If b <> "" Then
		b = split(b, "~")

		sID = trim(b(0))
        nip = trim(b(1))

		if sID <> "" AND nip <> "" then

            cek1_cmd.commandtext="SELECT * FROM OPR_T_eSuratJalan WHERE SJH_ID = '"& sID &"' AND (SJH_Sopir1_nip = '"& nip &"' OR SJH_Sopir2_nip = '"& nip &"')"
            set cek1 = cek1_cmd.execute

			if cek1.eof then
				cek2_cmd.commandtext="SELECT ISNULL(OPR_T_eSuratJalan.SJH_Sopir1_nip, '') AS SJH_Sopir1_nip, ISNULL(OPR_T_eSuratJalan.SJH_Sopir2_nip, '') AS SJH_Sopir2_nip FROM OPR_T_eSuratJalan WHERE SJH_ID = '"& sID &"'"
				set cek2 = cek2_cmd.execute

				if not cek2.eof then

					if cek2("SJH_Sopir1_nip") <> "" AND cek2("SJH_Sopir2_nip") = "" then
						insorupdate_cmd.commandtext="UPDATE OPR_T_eSuratJalan SET OPR_T_eSuratJalan.SJH_Sopir1_nip = '"& nip &"' WHERE SJH_ID = '"& sID &"'"
						set insorupdate = insorupdate_cmd.execute
					elseif cek2("SJH_Sopir1_nip") = "" AND cek2("SJH_Sopir2_nip") <> "" then
						insorupdate_cmd.commandtext="UPDATE OPR_T_eSuratJalan SET OPR_T_eSuratJalan.SJH_Sopir2_nip = '"& nip &"' WHERE SJH_ID = '"& sID &"'"
						set insorupdate = insorupdate_cmd.execute
					elseif cek2("SJH_Sopir1_nip") = "" AND cek2("SJH_Sopir2_nip") = "" then
						insorupdate_cmd.commandtext="UPDATE OPR_T_eSuratJalan SET OPR_T_eSuratJalan.SJH_Sopir1_nip = '"& nip &"' WHERE SJH_ID = '"& sID &"'"
						set insorupdate = insorupdate_cmd.execute
					end if

					response.ContentType = "application/json;charset=utf-8"
					Response.Write "{""SUCCESS"":""DONE"", ""FLAG"":""Y"", ""MESSAGE"":""Anda berhasil terdaftar pada surat tugas ("& sID &"), Selamat Bertugas!""}"

				else
					response.ContentType = "application/json;charset=utf-8"
					Response.Write "{""SUCCESS"":""DONE"", ""FLAG"":""N"", ""MESSAGE"":""Surat tugas ini "& sID &" tidak ada didalam sistem dakota!""}"
				end if
			else
				response.ContentType = "application/json;charset=utf-8"
				Response.Write "{""SUCCESS"":""DONE"", ""FLAG"":""N"", ""MESSAGE"":""Anda sudah terdaftar pada surat tugas ("& sID &") !""}"
			end if


		else
                response.ContentType = "application/json;charset=utf-8"
                Response.Write "{""ERROR"":""FAILED"", ""FLAG"":""N"", ""MESSAGE"":""Nomor Surat Tugas dan NIP anda tidak terbaca oleh sistem, silahkan coba melakukan scan / input manual kembali!""}"
		end if
	End If
%>
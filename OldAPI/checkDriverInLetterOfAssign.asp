<!--#include file="../../connection.asp" -->
<%

	Dim cbid, nourut, cek, cek_cmd, cek_absen, cek_absen_cmd, insorupdate, insorupdate_cmd
	set cek_cmd = server.CreateObject("ADODB.command")
	cek_cmd.activeConnection = MM_Cargo_String

    set cek_absen_cmd = server.CreateObject("ADODB.Command")
    cek_absen_cmd.ActiveConnection = MM_Cargo_string

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

            'MODE PRODUCTION
            m = right("00" & month(now),2)
            d = right("00" & day(now),2)
            y = right("0000" & year(now),4)
            
            'MODE DEVELOPMENT
            'm = 01
            'd = 01
            'y = 2024
            
            tgl = m & "/" & d & "/" & y

            cek_cmd.commandtext="SELECT ISNULL(OPR_T_eSuratJalan.SJH_Sopir1_nip, '') AS SJH_Sopir1_nip, ISNULL(OPR_T_eSuratJalan.SJH_Sopir2_nip, '') AS SJH_Sopir2_nip FROM OPR_T_eSuratJalan WHERE SJH_ID = '"& sID &"'"
            set cek = cek_cmd.execute

            cek_absen_cmd.CommandText = "SELECT DISTINCT Karyawan.Kry_NIP, Karyawan.Kry_Nama, Karyawan.Kry_Telp1, Karyawan.Kry_Telp2, FORMAT(CONVERT(DATETIME, MAX(Abs_datetime), 120), 'HH:mm') AS Abs_Time, Karyawan.Kry_PIN FROM HRD_M_Karyawan Karyawan LEFT OUTER JOIN HRD_T_Absensi AbsenKaryawan ON Karyawan.Kry_NIP = AbsenKaryawan.Abs_NIP AND CAST(AbsenKaryawan.Abs_datetime AS DATE) = '"& tgl &"' WHERE (AbsenKaryawan.Abs_SyncToAdempiere <> 'H') AND (Karyawan.Kry_AktifYN = 'Y') AND (Karyawan.Kry_NIP = '"& nip &"') GROUP BY Karyawan.Kry_NIP, Karyawan.Kry_Nama, Karyawan.Kry_Telp1, Karyawan.Kry_Telp2, Karyawan.Kry_PIN ORDER BY Karyawan.Kry_Nama"
					
			set cek_absen = cek_absen_cmd.execute

            if cek_absen.eof then
                response.ContentType = "application/json;charset=utf-8"
                Response.Write "{""SUCCESS"":""DONE"", ""FLAG"":""N"", ""MESSAGE"":""Anda belum melakukan absen masuk, silahkan absen terlebih dahulu!""}"
            elseif not cek_absen.eof AND cek_absen("Kry_PIN") <> "M" then
                response.ContentType = "application/json;charset=utf-8"
                Response.Write "{""SUCCESS"":""DONE"", ""FLAG"":""N"", ""MESSAGE"":""Anda belum melakukan absen masuk, silahkan absen terlebih dahulu!""}"
            elseif cek.eof then
                response.ContentType = "application/json;charset=utf-8"
                Response.Write "{""SUCCESS"":""DONE"", ""FLAG"":""N"", ""MESSAGE"":""Surat tugas ini "& sID &" tidak ada didalam sistem dakota!""}"
            else
                if cek("SJH_Sopir1_nip") <> "" AND cek("SJH_Sopir2_nip") <> "" then
                    response.ContentType = "application/json;charset=utf-8"
                    Response.Write "{""SUCCESS"":""DONE"", ""FLAG"":""N"", ""MESSAGE"":""Anda tidak bisa membawa barang yang ada pada surat tugas ini karena pengemudi 1 dan pengemudi 2 pada Surat Tugas ini sudah terisi!""}"
                elseif cek("SJH_Sopir1_nip") <> "" AND cek("SJH_Sopir2_nip") = "" then
                    response.ContentType = "application/json;charset=utf-8"
                    Response.Write "{""SUCCESS"":""DONE"", ""FLAG"":""Y"", ""MESSAGE"":""Apakah anda ingin membawa barang yang ada pada surat tugas ini "& sID &" ?""}"
                elseif cek("SJH_Sopir1_nip") = "" AND cek("SJH_Sopir2_nip") <> "" then
                    response.ContentType = "application/json;charset=utf-8"
                    Response.Write "{""SUCCESS"":""DONE"", ""FLAG"":""Y"", ""MESSAGE"":""Apakah anda ingin membawa barang yang ada pada surat tugas ini "& sID &" ?""}"
                else
                    response.ContentType = "application/json;charset=utf-8"
                    Response.Write "{""SUCCESS"":""DONE"", ""FLAG"":""Y"", ""MESSAGE"":""Apakah anda ingin membawa barang yang ada pada surat tugas ini "& sID &" ?""}"
                end if
            end if
		else
            response.ContentType = "application/json;charset=utf-8"
            Response.Write "{""ERROR"":""FAILED"", ""FLAG"":""N"", ""MESSAGE"":""Nomor Surat Tugas dan NIP anda tidak terbaca oleh sistem, silahkan coba melakukan scan / input manual kembali!""}"
		end if
	End If
%>
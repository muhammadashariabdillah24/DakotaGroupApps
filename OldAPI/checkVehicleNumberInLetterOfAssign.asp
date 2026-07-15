<!--#include file="../../connection.asp" -->   
<%

	Dim cbid, nourut, cek1, cek1_cmd, cek2, cek2_cmd, insorupdate, insorupdate_cmd
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

            cek1_cmd.commandtext="SELECT ISNULL(GLB_M_Kendaraan.Kend_ID, '') AS Kend_ID FROM GLB_M_Kendaraan WHERE GLB_M_Kendaraan.Kend_Sopir1 = '"& nip &"' OR GLB_M_Kendaraan.Kend_Sopir2 = '"& nip &"'"
            set cek1 = cek1_cmd.execute

            cek2_cmd.commandtext="SELECT ISNULL(OPR_T_eSuratJalan.SJH_KendID, '') AS SJH_KendID FROM OPR_T_eSuratJalan WHERE OPR_T_eSuratJalan.SJH_Sopir1_nip = '"& nip &"' OR OPR_T_eSuratJalan.SJH_Sopir2_nip = '"& nip &"'"
            set cek2 = cek2_cmd.execute

            if not cek1.eof then
                if cek1("Kend_ID") <> cek2("SJH_KendID") then

                    response.ContentType = "application/json;charset=utf-8"
                    Response.Write "{""SUCCESS"":""DONE"", ""FLAG"":""Y"", ""MESSAGE"":""Nomor mobil batangan anda ("& cek1("Kend_ID") &") dan nomor mobil yang ingin anda bawa ("& cek2("SJH_KendID") &") tidak sesuai, Apakah anda tetap ingin melanjutkan?""}"

                else
                    response.ContentType = "application/json;charset=utf-8"
                    Response.Write "{""SUCCESS"":""DONE"", ""FLAG"":""Y"", ""MESSAGE"":""Apakah anda ingin membawa barang dengan nomor mobil ("& cek2("SJH_KendID") &")?""}"
                end if
            else
                response.ContentType = "application/json;charset=utf-8"
                Response.Write "{""SUCCESS"":""DONE"", ""FLAG"":""Y"", ""MESSAGE"":""Apakah anda ingin membawa barang dengan nomor mobil ("& cek2("SJH_KendID") &")???""}"
            end if


		else
                response.ContentType = "application/json;charset=utf-8"
                Response.Write "{""ERROR"":""FAILED"", ""FLAG"":""N"", ""MESSAGE"":""Nomor Surat Tugas dan NIP anda tidak terbaca oleh sistem, silahkan coba melakukan scan / input manual kembali!""}"
		end if
	End If
%>
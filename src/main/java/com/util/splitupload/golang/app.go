package main

import (
	"bytes"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path"
	"strconv"
)

type FileInfo struct {
	id       string
	fileMd5  string
	filePath string
	fileName string
	fileSize int64
}

var uploadPath = "C:\\Users\\zhoudashuai\\Desktop\\split\\"
var fileInfoCache = map[string]FileInfo{}

func getSize(w http.ResponseWriter, r *http.Request) {
	if err := r.ParseMultipartForm(1024); err != nil {
		return
	}

	value, ok := fileInfoCache[r.FormValue("fileMd5")]

	w.Header().Set("Access-Control-Allow-Origin", "*")
	if ok {
		io.WriteString(w, strconv.FormatInt(value.fileSize, 10))
	} else {
		io.WriteString(w, "0")
	}
}

func upload(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	if err := r.ParseMultipartForm(1024); err != nil {
		return
	}

	fileName := r.FormValue("fileName")
	fileMd5 := r.FormValue("fileMd5")
	fileSize, _ := strconv.ParseInt(r.FormValue("fileSize"), 10, 64)
	position, _ := strconv.ParseInt(r.FormValue("position"), 10, 64)
	file, _, err := r.FormFile("file")

	if err != nil {
		log.Fatal(err)
		return
	}
	buf := bytes.NewBuffer(nil)
	if _, err := io.Copy(buf, file); err != nil {
		log.Fatal(err)
	}

	savedInfo, ok := fileInfoCache[fileMd5] //已存信息

	if ok {
		f, err := os.OpenFile(savedInfo.filePath, os.O_RDWR, os.ModePerm)
		if err != nil {
			log.Fatal(err)
		}
		defer f.Close()
		fi, _ := f.Stat()

		if fi.Size() == fileSize {
			fileInfoCache[savedInfo.fileMd5] = savedInfo
			w.WriteHeader(http.StatusCreated)
		} else {
			fmt.Println("ppp=>", position)
			f.Seek(position, io.SeekStart)
			f.Write(buf.Bytes())
			curSize := (savedInfo.fileSize + int64(buf.Len()))
			savedInfo.fileSize = curSize

			fileInfoCache[fileMd5] = savedInfo

			if curSize == fileSize {
				fileInfoCache[fileMd5] = savedInfo
				w.WriteHeader(http.StatusCreated)
			}
		}
	} else {
		path := uploadPath + fileMd5 + path.Ext(fileName)
		f, err := os.OpenFile(path, os.O_CREATE, os.ModePerm)
		if err != nil {
			log.Fatal(err)
		}
		defer f.Close()
		fi, _ := f.Stat()

		f.Seek(position, io.SeekStart)
		f.Write(buf.Bytes())

		savedInfoPrev := FileInfo{}

		savedInfoPrev.filePath = path
		savedInfoPrev.fileName = fi.Name()
		savedInfoPrev.fileSize = fi.Size()
		savedInfoPrev.fileMd5 = fileMd5

		fileInfoCache[fileMd5] = savedInfoPrev
	}
}

func main() {
	http.HandleFunc("/getSize", getSize)
	http.HandleFunc("/upload", upload)
	http.ListenAndServe(":8888", nil)
}

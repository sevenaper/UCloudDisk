/*
 * 文件/文件夹上传接口封装
 *
 * 调用示例:
 * 
 * var fileUploader = new FileUploader();
 * 
 * 上传文件
 * fileUploader.onFileUploadProgress = function (percent) { progressbar.value = percent; }
 * fileUploader.uploadFile(file, file_name, file_path);
 *
 * 上传文件夹 (只支持Chrome)
 * <input id="fileFolder" name="fileFolder" type="file" webkitdirectory>
 * fileFolder.onchange = function(e){ var files = fileFolder.files; fileUploader.uploadDirectory(files, ""); }
 *
 * 创建目录
 * createDirectory(dir_paths, parent_dir_path)  dir_paths 必须为数组
 * 将会在用户根目录下创建 parent_dir_path + '/' + dir_paths[i]
 * 
 * 注意：所有的 file_path 末尾不能有 /
 * 
 * on 开头的函数为回调，可以赋值成自定义函数
 * 默认的回调函数会在控制台打印服务器响应的数据
 * 
 *
 * version: 1.3
 * written by star
 * 
 */
function FileUploader() {

    this.onHashProgress = function (percent) {
        console.log(percent);
    };
    this.onHashComplete = function (file_hash) {
        console.log(file_hash);
    };
    this.onChunkUploadComplete = function (current_chunk, chunk_count, chunk_size) {
        console.log(current_chunk, chunk_count, chunk_size);
    };

    this.onDirUploadFileStart = function(file_path, file_name) {

    };

    this.onFileUploadProgress = function (percent) {
        console.log(percent);
    };
    this.onFileUploadComplete = function (response) {
        console.log(response);
    };
    this.onFileUploadError = function (response) {
        console.log(response);
    };

    this.onFileAddComplete = function (response, upload_method /*1=秒传*/) {
        console.log(response);
    };
    this.onFileAddError = function (response) {
        console.log(response);
    };

    this.onDirCreateComplete = function (response) {
        console.log(response);
    };
    this.onDirCreateError = function (response) {
        console.log(response);
    };

    this.onDirUploadComplete = function () {
        console.log("文件夹上传结束");
    };

    var server = '/interfaces/file';
    var hash;

    FileUploader.prototype.createDirectory = function (dir_paths, parent_dir_path) {
        for (var i = 0; i < dir_paths.length; i++) {
            dir_paths[i] = parent_dir_path + '/' + dir_paths[i];
        }
        var fileUploaderInstance = this;
        createDirectoryImpl(dir_paths, function (response) {
            if (response['status'] === 0) {
                fileUploaderInstance.onDirCreateComplete(response);
            } else {
                fileUploaderInstance.onDirCreateError(response);
            }
        });
    };

    FileUploader.prototype.uploadDirectory = function (files, parent_dir_path) {
        var dir_paths = [];
        var i;
        for (i = 0; i < files.length; i++) {
            var file = files[i];
            // 对文件路径截取得到所有目录路径
            var dir_path = file.webkitRelativePath.substring(0, file.webkitRelativePath.lastIndexOf('/'));
            if (dir_paths.indexOf(dir_path) === -1) dir_paths.push(dir_path);
			while (dir_path.lastIndexOf('/') !== -1)  {
				dir_path = dir_path.substring(0, dir_path.lastIndexOf('/'));
				if (dir_paths.indexOf(dir_path) === -1) dir_paths.push(dir_path);
			}
        }
        // 对目录路径排序，加上父目录路径
        dir_paths = dir_paths.sort();
        for (i = 0; i < dir_paths.length; i++) {
           dir_paths[i] = parent_dir_path + '/' + dir_paths[i];
        }
        var parent_dirs = [];
        var parent_dir = parent_dir_path;
        while (parent_dir.lastIndexOf('/') !== -1)  {
            parent_dir = parent_dir.substring(0, parent_dir.lastIndexOf('/'));
            if (parent_dir !== "" && parent_dirs.indexOf(parent_dir) === -1) parent_dirs.push(parent_dir);
        }
        dir_paths = parent_dirs.concat(dir_paths);
        console.log(dir_paths);

        var fileUploaderInstance = this;
        createDirectoryImpl(dir_paths, function (response) {
            if (response['status'] === 0) {
                fileUploaderInstance.onDirCreateComplete(response);

                // 上传所有文件
                var currentIndex = 0;

                function up() {
                    if (currentIndex >= files.length) {
                        fileUploaderInstance.onDirUploadComplete();
                        return;
                    }
                    var file = files[currentIndex];
                    var dir_path = parent_dir_path + '/' + file.webkitRelativePath.substring(0, file.webkitRelativePath.lastIndexOf('/'));
                    var file_name = file.webkitRelativePath.substring(file.webkitRelativePath.lastIndexOf('/') + 1);
                    fileUploaderInstance.onDirUploadFileStart(dir_path, file_name);
                    console.log(dir_path, file_name);
                    jQuery.when(fileUploaderInstance.uploadFile(file, file_name, dir_path)).done(function (response) {
                        currentIndex++;
                        up();
                    });
                }

                up();

            } else {
                fileUploaderInstance.onDirCreateError(response);
            }
        });
    };

    FileUploader.prototype.getHash = function (file) {
        getHashImpl(file, this.onHashComplete, this.onHashProgress);
    };

    FileUploader.prototype.uploadFile = function (file, file_name, file_path) {
        var dtd = jQuery.Deferred();
        var fileUploaderInstance = this;
        getHashImpl(file, function (hash) {
            fileUploaderInstance.onHashComplete(hash);
            var formData = new FormData();
            formData.append('hash', hash);
            formData.append('size', file.size);
            jQuery.ajax({
                url: server + '/upload_file_begin',
                type: 'post',
                processData: false,
                contentType: false,
                data: formData,
                success: function (data) {
                    if (data['status'] !== 0) {
                        fileUploaderInstance.onFileUploadError(data);
                    }
                    else if (data['result'] === 0) {
                        uploadFileDataImpl(file, function (errcode, msg, response) {
                            if (errcode === 0) {
                                // Finish file transmission
                                var formData = new FormData();
                                formData.append('hash', hash);
                                jQuery.ajax({
                                    url: server + '/upload_file_end',
                                    type: 'post',
                                    processData: false,
                                    contentType: false,
                                    data: formData,
                                    success: function (data) {
                                        fileUploaderInstance.onFileUploadProgress(100);
                                        fileUploaderInstance.onFileUploadComplete(data);
                                        addFile(file_name, file_path, file.size, function (response) {
                                            if (response['status'] !== 0) {
                                                fileUploaderInstance.onFileAddError(response);
                                                dtd.reject(response);
                                            } else {
                                                fileUploaderInstance.onFileAddComplete(response, 0);
                                                dtd.resolve(response);
                                            }
                                        });
                                    },
                                    error: function (xhr, textStatus, errorThrown) {
                                        fileUploaderInstance.onFileUploadError(xhr);
                                        dtd.reject(xhr);
                                    }
                                });
                            } else {
                                fileUploaderInstance.onFileUploadError(response);
                            }
                        }, fileUploaderInstance.onChunkUploadComplete, function (percent, complete_chunk_count, chunk_count) {
                            // 上传进度回调
                            fileUploaderInstance.onFileUploadProgress((percent / 100 + complete_chunk_count) / chunk_count * 100);
                        });
                    }
                    else if (data['result'] === 1) {
                        addFile(file_name, file_path, file.size, function (response) {
                            if (response['status'] !== 0) {
                                fileUploaderInstance.onFileAddError(response);
                                dtd.reject(response);
                            } else {
                                fileUploaderInstance.onFileAddComplete(response, 1);
                                dtd.resolve(response);
                            }
                        });
                    }
                },
                error: function (xhr, textStatus, errorThrown) {
                    fileUploaderInstance.onFileUploadError(xhr);
                    dtd.reject(xhr);
                }
            });
        }, this.onHashProgress);

        return dtd;
    };

    function getHashImpl(file, callback, progressCallback) {
        var blobSlice = File.prototype.slice || File.prototype.mozSlice || File.prototype.webkitSlice,
            chunkSize = 1048576, // Read in chunks of 1MB
            chunks = Math.ceil(file.size / chunkSize),
            currentChunk = 0,
            spark = new SparkMD5.ArrayBuffer(),
            fileReader = new FileReader();

        fileReader.onload = function (e) {
            spark.append(e.target.result); // Append array buffer
            if (chunks !== 0) {
                var md5_progress = Math.floor(100 * currentChunk / chunks);
                progressCallback(md5_progress);
            }
            currentChunk++;
            if (currentChunk < chunks) {
                loadNext();
            } else {
                hash = spark.end();
                callback(hash);
                progressCallback(100);
            }
        };

        fileReader.onerror = function () {
            console.warn('oops, something went wrong.');
        };

        function loadNext() {
            var start = currentChunk * chunkSize,
                end = ((start + chunkSize) >= file.size) ? file.size : start + chunkSize;
            fileReader.readAsArrayBuffer(blobSlice.call(file, start, end));
        }

        loadNext();
    }

    function uploadFileDataImpl(file, callback, chunkUploadCompleteCallback, progressCallback) {
        if (file.size <= 0) {
            callback(0, "file.size <= 0", null);
            return;
        }

        var blobSlice = File.prototype.slice || File.prototype.mozSlice || File.prototype.webkitSlice,
            chunkSize = 1048576, // Read in chunks of 1MB
            chunks = Math.ceil(file.size / chunkSize),
            currentChunk = 0,
            currentWritePos = 0,
            fileReader = new FileReader();

        fileReader.onload = function (e) {
            // Upload chunk data
            currentWritePos = currentChunk * chunkSize;
            var formData = new FormData();
            formData.append('hash', hash);
            formData.append('data', new Blob([e.target.result], {type: "application/octet-stream"}));
            formData.append('pos', currentWritePos);
            var copy_current_chunk = currentChunk;
            jQuery.ajax({
                url: server + '/upload_file_data',
                type: 'post',
                processData: false,
                contentType: false,
                data: formData,
                success: function (data) {
                    if (data['status'] !== 0) {
                        callback(data['status'], data['msg'], data);
                    } else {
                        currentChunk++;
                        if (currentChunk < chunks) {
                            loadNext();
                        } else {
                            callback(0, "All chunks complete", data);
                        }
                    }
                    chunkUploadCompleteCallback(copy_current_chunk, chunks, chunkSize);
                },
                error: function (xhr, textStatus, errorThrown) {
                    callback(xhr.readyState, textStatus, xhr);
                },
                xhr: function () {
                    let myXhr = jQuery.ajaxSettings.xhr();
                    if (myXhr.upload) {
                        myXhr.upload.addEventListener('progress', function (e) {
                            var loaded = e.loaded;
                            var total = e.total;
                            var percent = Math.floor(100 * loaded / total);
                            progressCallback(percent, copy_current_chunk + 1, chunks);
                        }, false);
                    }
                    return myXhr;
                }
            });

        };

        fileReader.onerror = function () {
            console.warn('oops, something went wrong.');
            callback(500, 'unable to read file', null);
        };

        function loadNext() {
            var start = currentChunk * chunkSize,
                end = ((start + chunkSize) >= file.size) ? file.size : start + chunkSize;
            fileReader.readAsArrayBuffer(blobSlice.call(file, start, end));
        }

        loadNext();
    }

    function addFile(file_name, file_path, file_size, callback) {
        var formData = new FormData();
        formData.append('hash', hash);
        formData.append('size', file_size);
        formData.append('filename', file_name);
        formData.append('filepath', file_path);
        jQuery.ajax({
            url: server + '/add_file',
            type: 'post',
            processData: false,
            contentType: false,
            data: formData,
            success: function (data) {
                callback(data);
            },
            error: function (xhr, textStatus, errorThrown) {
                callback(xhr);
            }
        });
    }

    function createDirectoryImpl(dir_full_paths, callback) {
        var formData = new FormData();
        formData.append("paths", dir_full_paths);
        jQuery.ajax({
            url: server + '/create_dir',
            type: 'post',
            processData: false,
            contentType: false,
            traditional: true,
            data: formData,
            success: function (data) {
                callback(data);
            },
            error: function (xhr, textStatus, errorThrown) {
                callback(xhr);
            }
        });
    }
}
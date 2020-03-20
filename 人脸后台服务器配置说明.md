# 服务器配置说明

## 一. 基本环境配置  flask + gunicorn+ nginx

### 1. python3, pip3

推荐Anaconda

### 2. flask安装

``` shell
pip install flask
```



### 3. gunicorn 安装（可选）

```shell
pip install gunicorn
```

### 4. nginx反向代理安装

``` 
sudo apt-get install nginx
```

### 5. 测试nginx

``` ngi
sudo nginx -t
```

窗口显示：

``` 
army@localhost:~$ sudo nginx -t
nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
nginx: configuration file /etc/nginx/nginx.conf test is successful
```

浏览器中访问ip地址显示：

``` 
Welcome to nginx!
If you see this page, the nginx web server is successfully installed and working. Further configuration is required.

For online documentation and support please refer to nginx.org.
Commercial support is available at nginx.com.

Thank you for using nginx.
```

初步配置成功！

## 二. 启动服务

### 1. 基于Gunicorn启动我们的Flask项目

``` 
gunicorn -w 4 -b 127.0.0.1:5000 app:app   #用gunicorn启动flask服务 

# -w 4              [开启4个进程]
# -b 127.0.0.1:5000 [启动flask服务的ip+端口]
# app         [启动flask服务的文件名.py]
# app               [项目实例]
```

启动Flask项目后查看Gunicorn进程命令

``` 
ps -ef | grep gun
```

### 或以模块运行

```
python -m flask run
```

### 2. 配置nginx 反向代理

在http{}中添加，server_name为服务器外网IP:

```
	upstream my_server{
		server localhost:5000;
		keepalive 2000;	
	}

	server {
		# 监听80端口
		listen 80;

		# 本机
		server_name 114.55.218.3; 
		index index.html;
		
		# 默认请求的url
		location = /{
			proxy_pass http://my_server;
			proxy_set_header Host $host; 
		}

		location  /pull{
			proxy_pass http://my_server/pull;
			proxy_set_header Host $host;
		}

		location  /update{
			proxy_pass http://my_server/update;
			proxy_set_header Host $host;
		}

		location  /delete{
			proxy_pass http://my_server/delete;
			proxy_set_header Host $host;
		}
		
		location /download{
			proxy_pass http://my_server/download;
			proxy_set_header Host $host;
		}

   	 }
```

测试并重启：

```
sudo nginx -t 
sudo service nginx restart 
```

### 3. 测试URL

```
curl 114.55.218.3/pull
```

若配置成功，则可获取到人脸数据。
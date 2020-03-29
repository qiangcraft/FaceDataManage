import requests

new_data = {'new_face': '曾强，你好'}
r = requests.post('http://192.168.0.105:80/update', data=new_data)

print(r.text)
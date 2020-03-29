from flask import Flask, request, send_from_directory,jsonify
import json
from os.path import join, dirname, realpath

# path of faces data file
PATH_OF_FILE = join(dirname(realpath(__file__)), 'static')
DATA_FILE = join(dirname(realpath(__file__)), 'static') + '/face_data'
ENCODE = 'utf-8'

app = Flask(__name__)


@app.route('/')
def hello_world():
    return 'Hello World!'


@app.route("/download/<filename>", methods=['GET'])
def download_file(filename):
    return send_from_directory(PATH_OF_FILE, filename, as_attachment=True)


@app.route('/update', methods=['post'])
def update_data():
    file = open(DATA_FILE, 'a', encoding=ENCODE)
    # print(request.headers)
    # print(request.form)
    # print(request.form['new_face'])
    new_data = request.form['new_face']
    file.write('\n')
    file.write(new_data)
    file.close()
    return jsonify({"msg": "成功更新数据！", "code": "200"})


@app.route('/pull')
def send_data():
    file = open(DATA_FILE, 'r', encoding=ENCODE)
    data_list = file.readlines()
    face_data = {}
    for p in data_list:
        temp = p.split(':')
        name = temp[0]
        data = temp[1]
        face_data[name] = data
        # face_data += p + '\r\n'
    face_data_json = json.dumps(face_data)
    return face_data_json


@app.route('/delete')
def delete_person():
    file = open(DATA_FILE, 'r', encoding=ENCODE)
    name = request.args.get("name")
    faces_list = file.readlines()
    file.close()

    length = len(faces_list)
    index = -1  # 删除的是第几行
    new_data = []
    is_exist = False # 将要删除的人物数据姓名是否存在

    for j, line in enumerate(faces_list):
        if line.startswith(name):
            is_exist = True
            index = j
            continue
        else:
            new_data.append(line)

    # 如果删除的是最后一行数据，删除前一行的空格
    print(length)
    if is_exist:
        if index == length - 1:
            last_line = new_data[-1][:-1]  # 新数据的最后一行去掉换行后。
            new_data = new_data[:-1]  # 除最后一行以外的行
            new_data.append(last_line)

        file = open(DATA_FILE, 'w', encoding=ENCODE)
        file.writelines(new_data)
        file.close()
        return jsonify({"msg": "成功删除"+name+"的人脸数据!", "code": "200"})
    return jsonify({"msg": "人脸库中无对应人物", "code": "100"})


if __name__ == '__main__':
    app.run()

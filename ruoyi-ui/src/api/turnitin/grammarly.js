import request from '@/utils/request';

const BASE_URL = '/grammarly';

export function submit(formData) {
  return request({
    url: BASE_URL + '/submit',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
      'repeatSubmit': 'false'
    }
  })
}

export function query(code) {
  return request({
    url: BASE_URL + '/submit',
    method: 'post',
    data: code
  })
}

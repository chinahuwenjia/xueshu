import request from '@/utils/request'

const BASE_URL = '/system/turnitin/teacher';

export function getAllTurnitinAccounts() {
  return request({
    url: BASE_URL,
    method: 'get'
  });
}


export function addTurnitinAccount(account) {
  return request({
    url: BASE_URL,
    method: 'post',
    data: account
  });
}

export function updateTurnitinAccount(account) {
  return request({
    url: BASE_URL,
    method: 'put',
    data: account
  });
}

export function deleteTurnitinAccount(id) {
  return request({
    url: BASE_URL + `/${id}`,
    method: 'post',
    params: id
  });
}

export function getAccountsByBusinessType(businessType) {
  if (businessType === 'null' || businessType === '')
    return getAllTurnitinAccounts();
  if (businessType === 'Turnitin'){

  }
  return request({
    url: `${BASE_URL}/accounts`,
    method: 'get',
    params: {businessType}
  });
}

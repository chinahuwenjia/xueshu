import request from '@/utils/request';

const BASE_URL = '/system/codes';

export function getAllCodes(params) {
  return request({
    url: BASE_URL + '/page',
    method: 'get',
    params
  });
}

export function getStats() {
  return request({
    url: BASE_URL + '/stats',
    method: 'get'});
}


export function addCode(code) {
  return request({
    url: BASE_URL + '/generate/batch',
    method: 'post',
    data: code
  });
}

export function deleteCodes(ids) {
  return request({
    url: BASE_URL + '/batchdelete',
    method: 'post',
    data: ids
  });
}

export function exportCodes() {
  return request({
    url: BASE_URL + '/export',
    method: 'get',
    responseType: 'blob'
  });
}



export function searchCodes(params) {
  return request({
    url: BASE_URL + '/search',
    method: 'get',
    params
  });
}

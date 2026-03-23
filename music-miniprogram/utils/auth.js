const { request } = require("./request")

async function login({ username, password }) {
  return request({
    url: "/api/auth/login",
    method: "POST",
    data: {
      username,
      password
    }
  })
}

async function register({ username, password }) {
  return request({
    url: "/api/auth/register",
    method: "POST",
    data: {
      username,
      password
    }
  })
}

module.exports = {
  login,
  register
}

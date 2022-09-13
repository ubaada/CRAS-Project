/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

"use strict";
let module = angular.module('AccountModule', ['ngResource']);

module.factory('AccEndPoint', function ($resource) {
    return $resource("http://localhost:9000/api/account");
});

module.controller('AccountController', function (AccEndPoint) {
    this.create = function (account) {
        var resp = AccEndPoint.save({}, account);
        resp.$promise.catch(function (response) {
            M.toast({html: 'An error occoured <br/> '});
        });

        resp.$promise.then(function () {
            M.toast({html: 'Successfully Added!'});
        });
    };
});


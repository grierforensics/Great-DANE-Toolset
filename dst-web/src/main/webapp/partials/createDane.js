app.controller('CreateDaneCtrl', function ($scope, $http) {

    $scope.reset = function () {
        $scope.result = null;
        $scope.email = null;
        $scope.certHex = null;
    };

    $scope.createDane = function () {
        $http.post('/toolset/' + encodeURI($scope.email) + '/dnsZoneLineForCert', $scope.certHex)
            .success(function (data) {
                $scope.result = data;
            })
            .error(function (data, status) {
                if (status == 400)
                    $scope.error = data;
                else
                    $scope.error = "";
            });
    };
});
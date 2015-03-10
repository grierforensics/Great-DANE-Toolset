app.controller('FetchDaneCtrl', function ($scope, $http, $routeParams) {

    $scope.email = $routeParams.email;

    $http.get('/toolset/' + encodeURI($scope.email) + '/text')
        .success(function (data) {
            $scope.certs = _.map(data, function (cert, index) {
                return {
                    "text": cert,
                    "hexUrl": '/toolset/' + encodeURI($scope.email) + '/hex/' + index,
                    "daneSmimeaUrl": '/toolset/' + encodeURI($scope.email) + '/dnsZoneLine/' + index
                };
            });
        })
        .error(function (data, status) {
            if (status == 404)
                $scope.notFound = true;
            else
                $scope.error = true;
        });
});
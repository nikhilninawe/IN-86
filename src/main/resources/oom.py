#!/usr/bin/env python

import requests

for i in range(10):
    response = requests.get("https://union-app-charlie.turvo.com/api/orders/oom")
    print("Received " + str(response.status_code))

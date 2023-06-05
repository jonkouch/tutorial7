import numpy as np
from scipy.signal import find_peaks
import pandas as pd

samples_array = []

def step_count(sample):
    global samples_array
    samples_array.append(sample)
    num_of_peaks = len(find_peaks(samples_array, height = 12, distance = 5)[0])
    return num_of_peaks



def reset():
    global samples_array
    samples_array = []

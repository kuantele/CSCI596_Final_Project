# USC CSCI 596 Final Project (Fall 2023)
This project is implemented in Java to create a model that recognizes license plates parallelly. 

## Objective
- Train our model by offering the capital English character and digits in **Penitentiary Gothic** font.

- Offer the images of license plates in California into our model to recognize license plates' number and turn then into text correctly.

- Use multi-threads to accerlerate the process.

- Report the performance profile to identify the bottle neck of the code and evaluate how multi-threads in this case can help us to speed up.

## Description
### Workflow
<p align="center">
   <img src="./Flowchart.png">
</p>

### Step 1: Training Data
<p align="center">
   <img src="./demo1.png" width=50% height=50%>
</p>

1. Feed our model with 4 images containing A-Z and 0-9 in **Penitentiary Gothic** font.

2. Convert them to grayscale.

3. Resize them to 20 x 43 pixels.

4. Make the character and digit as the key, and 1D integer array that stores the grayscale pixel value as a value.
   
   [255, 255, 64, 27, 128, ...., 0] length: 860

5. Append hard-coded character data to training Data to improve accuracy.

### Step 2: Test Data
<p align="center">
   <img src="./demo2.png" width=50% height=50%>
</p>

1. Load license plate images to trained model.

2. Crop out top and bottom margins.

3. Use edge detection to separate each character and digit.

4. Calculate grayscale value of each character and digit in a 1D integer array.

   [226, 240, 65, 29, 124, ...., 0] length: 860

5. Use 1NN method to find the nearest neighbor as the output character or digit.

6. Compare the output and the label into a file.

### Step 3: Multi-threads speed up
1. Incorporate multi threads method to train our models with data.

2. Recognize abundant of license plate images simultaneously.

3. Report the performance profile to identify the bottle neck of the code and evaluate how multi-threads in this case can help us to speed up.

## Resources

The code was based on and derived from
1. Youtube channel: Oggi AI - Java image filter
   - https://www.youtube.com/playlist?list=PLj8W7XIvO93p1v-f_eSP3yDu4PVK9Pbrt
2. GitHib Page: Joe James
   - https://github.com/joeyajames/Java/tree/master/ALPR
   - https://github.com/joeyajames/Java/tree/master/Image%20Filters

## Contributors

- Kuan-Te (Johnny) Lee
- Yi-Ning (Kenny) Lin
- Yi-Hsuan (Ashley) Chen
